// SPDX-License-Identifier: GPL-3.0-or-later
// Read-only game-memory adapter with a separate, fixed-tool built-in controller output.
#include "snap_runtime.h"
#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>
#include <glob.h>
#include <linux/input.h>
#include <sys/ioctl.h>
#include <cerrno>
#include <time.h>
namespace {
using Clock=std::chrono::steady_clock;
struct Range {uint64_t begin,end;bool write;};
struct Sweep {std::vector<Range> ranges;size_t index{};uint64_t cursor{},candidate{},owner{};bool ambiguous{};Clock::time_point retry{};};
struct Reader {
 int pid{},fd{-1}; uint64_t base{},anchor{},controller{};
 std::vector<Range> ranges;Sweep priority,rest;
 Clock::time_point maps_at{};
 EdenDsmodHostApi host{};uint64_t run_scene{},run_sequence{};uint64_t geometry_curve{};unsigned geometry_course{};float geometry_length{};std::string geometry_json;
 ~Reader(){if(fd>=0)::close(fd);}
 bool Maps(){
  std::ifstream in("/proc/"+std::to_string(pid)+"/maps");std::string line;std::vector<Range> next;uint64_t executable=0;unsigned matches=0;
  while(std::getline(in,line)){
   if(line.find("/memfd:HostMemory")==std::string::npos)continue;
   unsigned long long a,b;char permissions[8]{};
   if(std::sscanf(line.c_str(),"%llx-%llx %7s",&a,&b,permissions)!=3||permissions[0]!='r'||b<=a)continue;
   next.push_back({a,b,permissions[1]=='w'});
   if(permissions[2]=='x'&&b-a==0xa39000){executable=a;++matches;}
  }
  if(matches!=1||base&&base!=executable)return false;
  base=executable;ranges=std::move(next);maps_at=Clock::now();return true;
 }
 bool Mapped(uint64_t a,uint64_t n)const{
  if(!n||a>UINT64_MAX-n)return false;
  auto it=std::upper_bound(ranges.begin(),ranges.end(),a,[](uint64_t p,const Range&r){return p<r.begin;});
  return it!=ranges.begin()&&a>=(--it)->begin&&a+n<=it->end;
 }
 bool Read(uint64_t a,void*out,size_t n){return Mapped(a,n)&&pread(fd,out,n,off_t(a))==ssize_t(n);}
 template<class T>bool Read(uint64_t a,T&v){v={};return Read(a,&v,sizeof(v));}
 bool Init(int target){
  if(getuid()!=0||target<=0)return false;pid=target;
  std::ifstream cmd("/proc/"+std::to_string(pid)+"/cmdline");std::string name;std::getline(cmd,name,'\0');
  if(name!="dev.eden.eden_emulator"||!Maps())return false;
  fd=open(("/proc/"+std::to_string(pid)+"/mem").c_str(),O_RDONLY|O_CLOEXEC);if(fd<0)return false;
  host.userdata=this;host.main_base=base;
  host.is_mapped=[](void*p,uint64_t a,uint64_t n)->EdenDsmodBool{return static_cast<Reader*>(p)->Mapped(a,n);};
  host.read_memory=[](void*p,uint64_t a,void*out,size_t n)->EdenDsmodBool{return static_cast<Reader*>(p)->Read(a,out,n);};
  BuildScan(priority,true);BuildScan(rest,false);return true;
 }
 void BuildScan(Sweep& sweep,bool nearby){
  sweep={};uint64_t hint=0;Read(base+0xcfab68,hint);
  for(auto&r:ranges){
   if(!r.write||r.begin>=(uint64_t(1)<<38)||(r.begin<base+0x3600000&&r.end>base))continue;
   bool near=hint>128*1024*1024&&r.begin>=hint-128*1024*1024&&r.begin<=hint+128*1024*1024;
   if(near==nearby)sweep.ranges.push_back(r);
  }
 }
 bool Anchor(uint64_t scene,uint64_t& owner){
  auto observation=scn::ReadSnapCandidate(host,scene);auto scope=observation?scn::ReadSnapScope(host,*observation):std::nullopt;
  if(!scope)return false;owner=scope->owner;return true;
 }
 bool CurrentScene(uint64_t owner,uint64_t& scene){
  uint64_t vt=0,control=0;if(!Read(owner,vt))return false;
  unsigned offset=vt==base+0xc800b0?0x170:vt==base+0xc89f88?0x90:0;
  if(!offset||!Read(owner+offset,scene)||scene<32||!Read(owner+offset+8,control)||control!=scene-32)return false;
  uint64_t currentOwner=0;return Anchor(scene,currentOwner)&&currentOwner==owner;
 }
 void Discover(Sweep& sweep,bool nearby,unsigned budget){
  if(Clock::now()<sweep.retry)return;
  auto end=Clock::now()+std::chrono::microseconds(budget);uint8_t bytes[65536];size_t total=0;
  while(sweep.index<sweep.ranges.size()&&total<2*1024*1024){
   const auto&r=sweep.ranges[sweep.index];if(!sweep.cursor)sweep.cursor=r.begin;
   size_t n=std::min<uint64_t>(sizeof(bytes),r.end-sweep.cursor);
   if(n>=8&&Read(sweep.cursor,bytes,n))for(size_t off=0;off+8<=n;){
    auto*hit=static_cast<const uint8_t*>(std::memchr(bytes+off,int((base+0xc8a050)&255),n-off-7));if(!hit)break;
    size_t at=size_t(hit-bytes);off=at+1;if((sweep.cursor+at)&7)continue;uint64_t vt;std::memcpy(&vt,hit,8);
    if(vt!=base+0xc8a050)continue;uint64_t owner=0;
    if(Anchor(sweep.cursor+at,owner)){if(sweep.candidate&&sweep.candidate!=sweep.cursor+at)sweep.ambiguous=true;sweep.owner=owner;sweep.candidate=sweep.cursor+at;}
   }
   total+=n;sweep.cursor+=n;if(sweep.cursor>=r.end){++sweep.index;sweep.cursor=0;}
   if(Clock::now()>=end)break;
  }
  if(sweep.index==sweep.ranges.size()){
   uint64_t owner=0;if(sweep.candidate&&!sweep.ambiguous&&Anchor(sweep.candidate,owner)&&owner==sweep.owner){anchor=sweep.candidate;controller=owner;return;}
   BuildScan(sweep,nearby);sweep.retry=Clock::now()+std::chrono::seconds(nearby?1:5);
  }
 }
 std::string Geometry(const scn::SnapObservation& value){
  if(geometry_curve==value.curve&&geometry_course==value.course&&geometry_length==value.curve_length&&!geometry_json.empty())return geometry_json;
  std::vector<scn::Vec3> points;for(unsigned i=0;i<=40;i++){auto p=scn::ReadRailPosition(host,value.curve,value.curve_length*float(i)/40.f);if(!p)return "";points.push_back(*p);}
  uint64_t hash=1469598103934665603ull;for(auto&p:points)for(float v:p){uint64_t n=uint64_t(llround(v*100));for(unsigned i=0;i<8;i++){hash^=(n>>(i*8))&255;hash*=1099511628211ull;}}
  char part[192];std::snprintf(part,sizeof(part),",\"routeKey\":\"%u:%lld:%016llx\",\"routePoints\":[",value.course,llround(value.curve_length*100),(unsigned long long)hash);std::string result(part);
  for(unsigned i=0;i<points.size();i++){auto&p=points[i];std::snprintf(part,sizeof(part),"%s[%.5f,%.5f,%.5f]",i?",":"",p[0],p[1],p[2]);result+=part;}
  result+="]";geometry_curve=value.curve;geometry_course=value.course;geometry_length=value.curve_length;geometry_json=result;return result;
 }
 std::string Camera(const scn::SnapScope& scope,const scn::SnapObservation& value,uint64_t phase){
  // The scene shares its render context with either course controller. It writes
  // the primary camera through render-context+0x10 -> set[0].
  // The HUD world is a separate object. All process reads stay read-only.
  if(!value.world||(phase!=base+0x62e180&&phase!=base+0x630240))return "";
  for(unsigned attempt=0;attempt<2;attempt++){
   uint64_t world=0,worldControl=0,set=0,setControl=0,camera=0,cameraControl=0,vt=0,method=0,after=0;int64_t strong=-1;
   auto alive=[&](uint64_t control){return control>=0x10000&&!(control&7)&&Read(control+8,strong)&&strong>=0&&strong<10000;};
   if(!Read(scope.scene+0x1a8,world)||world!=scope.render||!Read(scope.scene+0x1b0,worldControl)||worldControl!=scope.render_control||!alive(worldControl)||
      !Read(world+0x10,set)||!Read(world+0x18,setControl)||!alive(setControl)||
      !Read(set,camera)||!Read(set+8,cameraControl)||camera<32||cameraControl!=camera-32||!alive(cameraControl)||
      !Read(camera,vt)||vt!=base+0xc79f80||!Read(vt+16,method)||method!=base+0x295170)return "";
   struct Pose {float values[24];} pose{},again{};
   if(!Read(camera+0xd0,pose)||!Read(camera+0xd0,again)||std::memcmp(&pose,&again,sizeof(pose)))continue;
   scn::Vec3 eye{},forward{},up{};for(unsigned i=0;i<3;i++){eye[i]=pose.values[i];forward[i]=pose.values[4+i]-eye[i];up[i]=pose.values[8+i];}
   auto valid=[](const scn::Vec3&v){return std::all_of(v.begin(),v.end(),[](float n){return std::isfinite(n)&&std::abs(n)<1e5f;});};
   float fov=pose.values[22],aspect=pose.values[23];if(!valid(eye)||!valid(forward)||!valid(up)||!std::isfinite(fov)||fov<.1f||fov>2.8f||!std::isfinite(aspect)||aspect<.5f||aspect>3.f)return "";
   float norm=0,upNorm=0,offset=0;for(unsigned i=0;i<3;i++){norm+=forward[i]*forward[i];upNorm+=up[i]*up[i];float d=eye[i]-(*value.world)[i];offset+=d*d;}
   if(norm<.01f||norm>1e6f||upNorm<.25f||upNorm>2.25f||offset>100.f)return "";
   norm=std::sqrt(norm);upNorm=std::sqrt(upNorm);float dot=0;for(unsigned i=0;i<3;i++){forward[i]/=norm;up[i]/=upNorm;dot+=forward[i]*up[i];}if(std::abs(dot)>.1f)return "";
   if(!Read(scope.scene+0x1a8,after)||after!=world||!Read(scope.scene+0x1b0,after)||after!=worldControl||!alive(worldControl)||
      !Read(world+0x10,after)||after!=set||!Read(world+0x18,after)||after!=setControl||!alive(setControl)||
      !Read(set,after)||after!=camera||!Read(set+8,after)||after!=cameraControl||!alive(cameraControl)||!Read(camera,after)||after!=vt)continue;
   char out[330];std::snprintf(out,sizeof(out),",\"camera\":{\"forward\":[%.6f,%.6f,%.6f],\"up\":[%.6f,%.6f,%.6f],\"fovY\":%.6f,\"aspect\":%.6f}",forward[0],forward[1],forward[2],up[0],up[1],up[2],fov,aspect);return out;
  }return "";
 }
 std::string Sample(){
  if(Clock::now()-maps_at>std::chrono::seconds(1)&&!Maps())return "{\"status\":\"unavailable\"}";
  uint64_t scene=0,after=0;
  if(controller&&!CurrentScene(controller,scene)){controller=anchor=0;BuildScan(priority,true);BuildScan(rest,false);}
  if(!controller){Discover(priority,true,800);if(!controller)Discover(rest,false,200);if(!controller)return "{\"status\":\"seeking\"}";}
  if(!CurrentScene(controller,scene))return "{\"status\":\"idle\"}";
  auto sample=scn::ReadSnapCandidate(host,scene);auto scope=sample?scn::ReadSnapScope(host,*sample):std::nullopt;
  if(!scope||scope->owner!=controller)return "{\"status\":\"idle\"}";
  if(run_scene!=scene){run_scene=scene;++run_sequence;geometry_curve=0;}
  uint64_t phase=0;Read(scene+0x38,phase);char out[1024];
  int n=std::snprintf(out,sizeof(out),"{\"status\":\"ready\",\"courseId\":%u,\"level\":%u,\"distance\":%.6f,\"length\":%.6f,\"phaseRva\":%llu,\"forward\":[%.6f,%.6f,%.6f]",sample->course,sample->selected_level,sample->distance,sample->curve_length,(unsigned long long)(phase-base),sample->forward[0],sample->forward[1],sample->forward[2]);
  std::string json(out,size_t(std::max(0,n)));json+=",\"run\":"+std::to_string(run_sequence);
  if(sample->world){auto&w=*sample->world;std::snprintf(out,sizeof(out),",\"world\":[%.6f,%.6f,%.6f]",w[0],w[1],w[2]);json+=out;}
  json+=Geometry(*sample);json+=Camera(*scope,*sample,phase);
  if(!scn::SameSnapScope(host,*scope,*sample)||!Read(scene+0x80,after)||after!=sample->rail||!Read(sample->rail+0x30,after)||after!=sample->curve)return "{\"status\":\"idle\"}";
  auto remaining=scn::ReadRemainingPhotos(host,*scope,*sample);if(remaining)json+=",\"remainingPhotos\":"+std::to_string(*remaining);
  return json+"}";
 }
};
std::unique_ptr<Reader> reader;
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_gbppimentel_snapbridge_GameMemoryReader_open(JNIEnv*,jclass,jint pid){
 reader.reset(new Reader());if(reader->Init(pid))return JNI_TRUE;reader.reset();return JNI_FALSE;
}
extern "C" JNIEXPORT jstring JNICALL Java_dev_gbppimentel_snapbridge_GameMemoryReader_sample(JNIEnv*env,jclass){
 auto start=Clock::now();std::string json=reader?reader->Sample():"{\"status\":\"unavailable\"}";
 json.pop_back();json+=",\"readUs\":"+std::to_string(std::chrono::duration_cast<std::chrono::microseconds>(Clock::now()-start).count())+"}";return env->NewStringUTF(json.c_str());
}
extern "C" JNIEXPORT void JNICALL Java_dev_gbppimentel_snapbridge_GameMemoryReader_close(JNIEnv*,jclass){reader.reset();}

namespace {
// This descriptor targets only the physical controller port, never /proc/.../mem.
std::string BuiltInController(){
 glob_t names{};if(glob("/sys/class/input/event*/device/name",0,nullptr,&names))return {};
 std::string device;unsigned matches=0;for(size_t i=0;i<names.gl_pathc;i++){std::ifstream f(names.gl_pathv[i]);std::string name;std::getline(f,name);if(name=="Odin Controller"){std::string p=names.gl_pathv[i];auto a=p.find("event"),b=p.find('/',a);device="/dev/input/"+p.substr(a,b-a);matches++;}}globfree(&names);return matches==1?device:std::string{};
}
bool ToolCode(int code){return code==305||code==308||code==311||code==307;}
bool Released(int fd,int code){unsigned char keys[(KEY_MAX+8)/8]{};return ioctl(fd,EVIOCGKEY(sizeof(keys)),keys)>=0&&!(keys[code/8]&(1u<<(code%8)));}
bool WriteKey(int fd,int code,bool down){input_event events[2]{};events[0].type=EV_KEY;events[0].code=uint16_t(code);events[0].value=down?1:0;events[1].type=EV_SYN;events[1].code=SYN_REPORT;return write(fd,events,sizeof(events))==ssize_t(sizeof(events));}
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_gbppimentel_snapbridge_GameMemoryReader_physicalReleased(JNIEnv*,jclass,jint code){
 if(getuid()!=0||!ToolCode(code))return JNI_FALSE;auto device=BuiltInController();if(device.empty())return JNI_FALSE;
 int fd=open(device.c_str(),O_RDONLY|O_NONBLOCK|O_CLOEXEC);if(fd<0)return JNI_FALSE;bool result=Released(fd,code);::close(fd);return result?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_gbppimentel_snapbridge_GameMemoryReader_pressTool(JNIEnv*,jclass,jint code){
 if(getuid()!=0||!reader||!ToolCode(code))return JNI_FALSE;
 uint64_t scene=0,phase=0;
 if(!reader->CurrentScene(reader->controller,scene)||!reader->Read(scene+0x38,phase)||phase!=reader->base+0x62e180)return JNI_FALSE;
 auto device=BuiltInController();if(device.empty())return JNI_FALSE;int fd=open(device.c_str(),O_RDWR|O_NONBLOCK|O_CLOEXEC);if(fd<0)return JNI_FALSE;
 if(!Released(fd,code)){::close(fd);return JNI_FALSE;}
 bool down=WriteKey(fd,code,true);if(down){timespec duration{0,80000000};while(nanosleep(&duration,&duration)<0&&errno==EINTR){}}
 // Always emit the release, including after a short/failed down write. No held-key API exists.
 bool up=WriteKey(fd,code,false);for(int i=0;!up&&i<2;i++){timespec retry{0,1000000};nanosleep(&retry,nullptr);up=WriteKey(fd,code,false);}
 ::close(fd);return down&&up?JNI_TRUE:JNI_FALSE;
}
