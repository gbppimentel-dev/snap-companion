"""Offline geometry regressions; run with the same NumPy/SciPy environment as regeneration."""
import unittest
import numpy as np
from route_alignment import project,trace_alignment,smooth_runs
class AlignmentTests(unittest.TestCase):
 def test_height_projection_and_finite_bounds(self):
  points=np.array([[1.,2.,3.]])
  np.testing.assert_allclose(project(points,[2,0,0,2,1,1]),[[3,7]])
  np.testing.assert_allclose(project(points,[2,0,0,3,0,2,1,1]),[[3,13]])
  with self.assertRaises(ValueError):project(points,[2,0,0,2,np.nan,1])
 def test_trace_is_ordered_and_hits_explicit_fork(self):
  ts=np.linspace(0,10,101);guide=np.c_[ts*10,ts*0+8];trace=dict(polyline=[[0,0],[50,0],[100,10]],ties=[[0,0],[5,1],[10,2]])
  result=trace_alignment(ts,guide,trace)
  self.assertTrue(np.all(np.diff(result[:,0])>0));np.testing.assert_allclose(result[[0,50,100]],trace['polyline'],atol=1e-6)
 def test_invalid_trace_never_gets_silently_sorted_or_clipped(self):
  ts=np.linspace(0,10,30);guide=np.c_[ts,ts]
  for trace in [dict(polyline=[[0,0],[10,10]],ties=[[0,0],[10,0]]),dict(polyline=[[0,0],[10,10]],ties=[[0,0],[10,2]]),dict(polyline=[[0,0],[1000,10]],ties=[[0,0],[10,1]])]:
   with self.assertRaises(ValueError):trace_alignment(ts,guide,trace)
 def test_smoothing_reduces_ripple_and_preserves_gap_endpoints_and_displacement_bound(self):
  t=np.linspace(0,20,200);xy=np.c_[t*5,40+np.sin(t*7)*1.5];valid=np.ones(len(t),dtype=bool);valid[80:90]=False;result=smooth_runs(t,xy,valid)
  np.testing.assert_array_equal(result[80:90],xy[80:90]);np.testing.assert_allclose(result[[0,79,90,199]],xy[[0,79,90,199]],atol=1e-8);self.assertLessEqual(np.linalg.norm(result-xy,axis=1).max(),3.000001);self.assertLess(np.sum(np.diff(result[:80,1],n=2)**2),np.sum(np.diff(xy[:80,1],n=2)**2)*.25)
if __name__=='__main__':unittest.main()
