#!/usr/bin/env python3
"""Compare SCN curve math with the owner's unmodified original getter in an isolated VM.
Synthetic curve objects only. No device, gameplay process, saves, hooks or guest writes.
Requires pyelftools and Unicorn. No executable bytes are emitted or included in the project.
"""
import argparse
import ctypes
import hashlib
import json
from pathlib import Path
import random
import struct
import sys
from elftools.elf.elffile import ELFFile
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn.arm64_const import UC_ARM64_REG_X0,UC_ARM64_REG_X30,UC_ARM64_REG_SP,UC_ARM64_REG_S0,UC_ARM64_REG_Q0,UC_ARM64_REG_CPACR_EL1

ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--elf',type=Path,required=True);p.add_argument('--report',type=Path,required=True);a=p.parse_args()
    vm=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    with a.elf.open('rb') as f:
        elf=ELFFile(f)
        for seg in elf.iter_segments():
            if seg['p_type']=='PT_LOAD':
                base=seg['p_vaddr'];size=(seg['p_memsz']+4095)&~4095;vm.mem_map(base,size);vm.mem_write(base,seg.data())
        text=next(s.data() for s in elf.iter_segments() if s['p_type']=='PT_LOAD' and s['p_flags']&1)
        for sec in elf.iter_sections():
            if sec['sh_type']=='SHT_RELA':
                for r in sec.iter_relocations():
                    if r['r_info_type']==1027:vm.mem_write(r['r_offset'],struct.pack('<Q',r['r_addend']))
    base=0x40000000;vm.mem_map(base,0x10000);vm.mem_map(0x80000000,0x20000);stop=0x90000000;vm.mem_map(stop,4096)
    vm.reg_write(UC_ARM64_REG_CPACR_EL1,3<<20)
    lib=ctypes.CDLL(str(ROOT/'build/host'/('scn-curve-probe.dylib' if sys.platform=='darwin' else 'scn-curve-probe.so')))
    fn=lib.scn_probe_curve;fn.argtypes=[ctypes.c_void_p,ctypes.c_size_t,ctypes.c_uint64,ctypes.c_uint64,ctypes.c_float,ctypes.c_void_p];fn.restype=ctypes.c_int
    rng=random.Random(11018);cases=0;peak=0.
    for interpolation in [0,1]:
        for count in [2,3,7]:
            for trial in range(4):
                data=bytearray(0x10000);part=0x1000;points=0x2000;length=float((count-1)*10)
                struct.pack_into('<fQQ',data,0x3c,length,base+part,base+part+56)
                struct.pack_into('<I',data,0x70,0);struct.pack_into('<f',data,part+4,length)
                struct.pack_into('<fQQ',data,part+20,length,base+points,base+points+count*48)
                struct.pack_into('<I',data,part+48,interpolation)
                for i in range(count):
                    pos=[i*10.,rng.uniform(-10,10),rng.uniform(-20,20)];tangent=[rng.uniform(-1,1) for _ in range(3)]
                    struct.pack_into('<3f',data,points+i*48,*pos);struct.pack_into('<3f',data,points+i*48+16,*tangent)
                    struct.pack_into('<2f',data,points+i*48+32,min(i+1,count-1)*10.,10. if i<count-1 else 0.)
                vm.mem_write(base,bytes(data));native=ctypes.create_string_buffer(bytes(data));out=(ctypes.c_float*3)()
                for distance in [0.,1e-8,0.1,4.,9.9,10.,min(10.1,length),length*0.7,length-0.01,length]:
                    vm.reg_write(UC_ARM64_REG_SP,0x80010000);vm.reg_write(UC_ARM64_REG_X0,base);vm.reg_write(UC_ARM64_REG_S0,struct.unpack('<I',struct.pack('<f',distance/length))[0]);vm.reg_write(UC_ARM64_REG_X30,stop)
                    vm.emu_start(0x3e5a70,stop,count=10000)
                    expected=struct.unpack('<4f',int(vm.reg_read(UC_ARM64_REG_Q0)).to_bytes(16,'little'))[:3]
                    assert fn(native,len(data),base,base,distance,out)==1,('SCN rejected',interpolation,count,distance)
                    error=max(abs(float(out[i])-expected[i]) for i in range(3));peak=max(peak,error)
                    assert error<0.0001,('Curve mismatch',interpolation,count,distance,list(out),expected,error)
                    cases+=1
    report=dict(scope='Original game getter against synthetic curve objects in an isolated VM; no runtime/Thor validation',cases=cases,max_absolute_error=peak,original_getter='0x3e5a70',getter_sha256=hashlib.sha256(text[0x3e5a70:0x3e5c98]).hexdigest(),interpolation=['cubic Hermite','linear'])
    a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
