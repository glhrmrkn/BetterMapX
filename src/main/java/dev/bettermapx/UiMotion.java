package dev.bettermapx;

/** Small deterministic UI curves; no allocation or animation library required. */
final class UiMotion {
    private UiMotion() {}
    static float easeOutCubic(float progress) {
        float f = 1f - Math.clamp(progress, 0f, 1f);
        return 1f - f * f * f;
    }
    static float easeOutQuint(float progress) {
        float f = 1f - Math.clamp(progress, 0f, 1f);
        return 1f - f * f * f * f * f;
    }
    /** Critically damped smooth spring response without exaggerated bounce or oscillation. */
    static float smoothSpring(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        return 1f - (float)(Math.exp(-8.0 * t) * (1.0 + 8.0 * t));
    }
    static float cubicOut(float progress) {
        return easeOutCubic(progress);
    }
    /** Legacy spring with tiny overshoot retained for compatibility. */
    static float spring(float progress) {
        return smoothSpring(progress);
    }
    static float smooth(float current,float target,float dt,float speed){
        return current+(target-current)*(float)(1.0-Math.exp(-Math.max(0.0,dt)*speed));
    }
    static double smooth(double current,double target,double dt,double speed){
        return current+(target-current)*(1.0-Math.exp(-Math.max(0.0,dt)*speed));
    }
    static int alpha(int argb,float opacity) {
        return ((int)(((argb>>>24)&255)*Math.clamp(opacity,0f,1f))<<24)|(argb&0x00ffffff);
    }
    static int mix(int a,int b,float t){
        t=Math.clamp(t,0f,1f);
        int aa=(a>>>24)&255,ar=(a>>>16)&255,ag=(a>>>8)&255,ab=a&255;
        int ba=(b>>>24)&255,br=(b>>>16)&255,bg=(b>>>8)&255,bb=b&255;
        int oa=Math.round(aa+(ba-aa)*t),or=Math.round(ar+(br-ar)*t),og=Math.round(ag+(bg-ag)*t),ob=Math.round(ab+(bb-ab)*t);
        return oa<<24|or<<16|og<<8|ob;
    }
}
