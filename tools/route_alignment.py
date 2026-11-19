"""Offline schematic alignment helpers. Never reads runtime state or saves."""
import numpy as np
from scipy.interpolate import PchipInterpolator, UnivariateSpline

def project(points,matrix):
    m=np.asarray(matrix,dtype=float)
    if not np.isfinite(m).all():raise ValueError('Non-finite projection')
    if m.shape==(6,):return points[:,[0,2]]@m[:4].reshape(2,2)+m[4:]
    if m.shape==(8,):return points@m[:6].reshape(3,2)+m[6:]
    raise ValueError('Projection requires six or eight finite coefficients')

def trace_alignment(ts,guide,trace):
    """Monotone matching to a reviewed trace; explicit ties fix branch landmarks.

    Bounded 1:1, 1:2 and 2:1 steps prevent nearest-pixel jumps/backtracking.
    Coordinates come from a shape-preserving artist trace, never a clock.
    """
    poly=np.asarray(trace['polyline'],dtype=float);ties=np.asarray(trace['ties'],dtype=float)
    if poly.ndim!=2 or poly.shape[1]!=2 or not np.isfinite(poly).all() or np.any(poly<0) or np.any(poly>[959,539]):raise ValueError('Trace bounds')
    arc=np.r_[0,np.cumsum(np.linalg.norm(np.diff(poly,axis=0),axis=1))]
    if np.any(np.diff(arc)<=0) or len(ties)<2 or not np.isfinite(ties).all() or np.any(ties[:,1]<0) or np.any(ties[:,1]>=len(poly)) or np.any(ties[:,1]!=np.floor(ties[:,1])) or np.any(np.diff(ties[:,0])<=0) or np.any(np.diff(ties[:,1])<=0) or ties[0,0]!=0 or abs(ties[-1,0]-ts[-1])>1e-4:raise ValueError('Trace landmark order')
    curve=PchipInterpolator(arc,poly,axis=0);matched=np.full(len(ts),np.nan)
    for (d0,k0),(d1,k1) in zip(ties,ties[1:]):
        n=max(24,min(600,round((d1-d0)*3)));d=np.linspace(d0,d1,n);s=np.linspace(arc[int(k0)],arc[int(k1)],n)
        source=np.stack([np.interp(d,ts,guide[:,i])for i in range(2)],axis=1);target=curve(s)
        cost=np.sum((source[:,None]-target[None,:])**2,axis=2)
        ii,jj=np.indices((n,n));cost+=100*((ii-jj)/(n-1))**2
        dp=np.full((n,n),np.inf);prev=np.zeros((n,n),dtype=np.int8);dp[0,0]=cost[0,0]
        for i in range(1,n):
            for j in range(max(1,i-n//2),min(n,i+n//2+1)):
                candidates=[dp[i-1,j-1],dp[i-1,j-2]+9 if j>1 else np.inf,dp[i-2,j-1]+9 if i>1 else np.inf];choice=int(np.argmin(candidates));dp[i,j]=cost[i,j]+candidates[choice];prev[i,j]=choice
        if not np.isfinite(dp[-1,-1]):raise ValueError('Trace matching failed')
        i=j=n-1;path=[]
        while i or j:
            path.append((i,j));choice=prev[i,j];i-=2 if choice==2 else 1;j-=2 if choice==1 else 1
        path.append((0,0));path.reverse();a=np.asarray(path);mapping=PchipInterpolator(d[a[:,0]],s[a[:,1]])
        take=(ts>=d0-1e-7)&(ts<=d1+1e-7);matched[take]=mapping(np.clip(ts[take],d0,d1))
    if not np.isfinite(matched).all() or np.any(np.diff(matched)<-1e-6):raise ValueError('Non-monotone trace mapping')
    return curve(matched)

def smooth_runs(ts,xy,valid,tolerance=1.5):
    """Remove dashed-stroke/arrowhead ripple without crossing a rejected interval."""
    out=xy.copy();starts=np.flatnonzero(valid&~np.r_[False,valid[:-1]]);ends=np.flatnonzero(valid&~np.r_[valid[1:],False])+1
    for lo,hi in zip(starts,ends):
        if hi-lo<6:continue
        x=ts[lo:hi];base=xy[lo:hi];candidate=np.stack([UnivariateSpline(x,base[:,k],s=(hi-lo)*tolerance**2,k=3)(x)for k in range(2)],axis=1)
        # Keep endpoints fixed and bound every displacement, not only the mean fit.
        t=(x-x[0])/(x[-1]-x[0]);candidate+=(1-t[:,None])*(base[0]-candidate[0])+t[:,None]*(base[-1]-candidate[-1]);delta=candidate-base;length=np.linalg.norm(delta,axis=1);out[lo:hi]=base+delta*np.minimum(1,3/np.maximum(length,1e-9))[:,None]
    return out
