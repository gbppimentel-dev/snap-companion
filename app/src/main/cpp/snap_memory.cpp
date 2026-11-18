// SPDX-License-Identifier: GPL-3.0-or-later
// External-process adapter for the existing read-only Snap reader. No game writes/input hooks.
#include "snap_runtime.h"
#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>
namespace {
using Clock=std::chrono::steady_clock;
struct Range {uint64_t begin,end;bool write;};
struct Sweep {std::vector<Range> ranges;size_t index{};uint64_t cursor{},candidate{},owner{};bool ambiguous{};Clock::time_point retry{};};
struct Reader {
 int pid{},fd{-1}; uint64_t base{},wrapper{};
 std::vector<Range> ranges;Sweep priority,rest;
 Clock::time_point maps_at{};
 EdenDsmodHostApi host{};
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
 bool Loader(uint64_t w,uint64_t& loader){
  uint64_t vt=0;return !(w&7)&&Read(w,vt)&&vt==base+0xc80078&&Read(w+8,loader)&&loader>=0x10000&&!(loader&7)&&Read(loader,vt)&&vt==base+0xc800b0;
 }
 void Discover(Sweep& sweep,bool nearby,unsigned budget){
  if(Clock::now()<sweep.retry)return;
  auto end=Clock::now()+std::chrono::microseconds(budget);uint8_t bytes[65536];size_t total=0;
  while(sweep.index<sweep.ranges.size()&&total<2*1024*1024){
   const auto&r=sweep.ranges[sweep.index];if(!sweep.cursor)sweep.cursor=r.begin;
   size_t n=std::min<uint64_t>(sizeof(bytes),r.end-sweep.cursor);
   if(n>=8&&Read(sweep.cursor,bytes,n))for(size_t off=0;off+8<=n;){
    auto*hit=static_cast<const uint8_t*>(std::memchr(bytes+off,int((base+0xc80078)&255),n-off-7));if(!hit)break;
    size_t at=size_t(hit-bytes);off=at+1;if((sweep.cursor+at)&7)continue;uint64_t vt;std::memcpy(&vt,hit,8);
    if(vt!=base+0xc80078)continue;uint64_t owner=0;
    if(Loader(sweep.cursor+at,owner)){if(sweep.owner&&sweep.owner!=owner)sweep.ambiguous=true;sweep.owner=owner;sweep.candidate=sweep.cursor+at;}
   }
   total+=n;sweep.cursor+=n;if(sweep.cursor>=r.end){++sweep.index;sweep.cursor=0;}
   if(Clock::now()>=end)break;
  }
  if(sweep.index==sweep.ranges.size()){
   uint64_t owner=0;if(sweep.candidate&&!sweep.ambiguous&&Loader(sweep.candidate,owner)&&owner==sweep.owner){wrapper=sweep.candidate;return;}
   BuildScan(sweep,nearby);sweep.retry=Clock::now()+std::chrono::seconds(nearby?1:5);
  }
 }
 std::string Sample(){
  if(Clock::now()-maps_at>std::chrono::seconds(1)&&!Maps())return "{\"status\":\"unavailable\"}";
  uint64_t owner=0;
  if(wrapper&&!Loader(wrapper,owner)){wrapper=0;BuildScan(priority,true);BuildScan(rest,false);}
  if(!wrapper){Discover(priority,true,800);if(!wrapper)Discover(rest,false,200);if(!wrapper)return "{\"status\":\"seeking\"}";}
  uint64_t scene=0,control=0,parent=0,after=0;
  if(!Loader(wrapper,owner)||!Read(owner+0x170,scene)||!Read(owner+0x178,control)||scene<32||control!=scene-32)return "{\"status\":\"idle\"}";
  auto sample=scn::ReadSnapCandidate(host,scene);
  if(!sample||!Read(scene+0x238,parent)||parent!=owner||!Read(owner+0x170,after)||after!=scene)return "{\"status\":\"idle\"}";
  uint64_t phase=0;Read(scene+0x38,phase);char out[1024];
  int n=std::snprintf(out,sizeof(out),"{\"status\":\"ready\",\"courseId\":%u,\"level\":%u,\"distance\":%.6f,\"length\":%.6f,\"phaseRva\":%llu,\"forward\":[%.6f,%.6f,%.6f]",sample->course,sample->selected_level,sample->distance,sample->curve_length,(unsigned long long)(phase-base),sample->forward[0],sample->forward[1],sample->forward[2]);
  std::string json(out,size_t(std::max(0,n)));
  if(sample->world){auto&w=*sample->world;std::snprintf(out,sizeof(out),",\"world\":[%.6f,%.6f,%.6f]",w[0],w[1],w[2]);json+=out;}
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
