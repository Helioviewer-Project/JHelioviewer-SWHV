#version 110

uniform sampler2D image;
uniform float truncationValue;
uniform float isdifference;
uniform sampler2D differenceImage;
uniform vec4 pixelSizeWeighting;
uniform vec4 rect;
uniform float gamma;
uniform float contrast;
uniform sampler1D lut;
uniform float alpha;
uniform vec4 cutOffRadius;
uniform vec4 outerCutOffRadius;
uniform float phi;
uniform float theta;
varying vec4 outPosition;
uniform mat4 cameraTransformationInverse;
uniform mat4 layerLocalRotation;
uniform mat4 currentDragRotation;
uniform float physicalImageWidth;
uniform vec2 sunOffset;
uniform vec2 viewport;
uniform float vpheight;

struct Sphere{
    float radius;
    vec3 center;
};

struct Ray{
    vec3 direction;
    vec3 origin;
};

struct Plane{
    vec3 normal;
};

Sphere sphere = Sphere(1., vec3(0.,0.,0.));
Plane plane = Plane(vec3(0., 0., 1.));
float intersectSphere(in Ray ray, in Sphere sphere)
{
    float t = -1.;
    vec3 L =  sphere.center - ray.origin; 
    float tca = dot(L, ray.direction);
    float dsq = dot(L, L) - tca * tca;
    float diff = sphere.radius*sphere.radius - dsq;
    if (diff <= 0.0||tca<0.) {
        return t;
    }
    t = tca + sqrt(diff);
    return t;      
}

float intersectPlane(vec4 vecin)
{   
    vec3 altnormal = (currentDragRotation * vec4(0., 0., 1., 1.)).xyz;
    return -dot(altnormal.xy,vecin.xy)/altnormal.z;
}

void main(void)
{  
    vec2 normalizedScreenpos = 2.*((gl_FragCoord.xy/viewport)-0.5);

    normalizedScreenpos.y = normalizedScreenpos.y;

    vec4 up2 =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, 1., 1.);
    vec4 up1 =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);
    vec3 direction = (up1 - up2).xyz;
    vec3 newdirection = normalize(-direction);
    vec3 origin = up1.xyz;    
    
    Ray ray = Ray(newdirection, origin);

    //float tSphere = intersectSphere(ray, sphere);
    //float tPlane = intersectPlane(ray, plane);
  
    vec4 imageColor;
    vec3 hitPoint = vec3(up1.x, up1.y, sqrt(1.-dot(up1.xy, up1.xy)));
    vec4 rotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) * currentDragRotation;
    if(dot(up1.xy, up1.xy)<1. && dot(rotatedHitPoint.xyz, vec3(0.,0.,1.))>0.){
        vec4 lutg = texture1D(lut, 0.);
        vec2 texcoord = vec2((rotatedHitPoint.x*rect.z - rect.x*rect.z), (rotatedHitPoint.y*rect.w*1.0-rect.y*rect.w)); 
        imageColor = texture2D(differenceImage, texcoord);
        imageColor = texture2D(image, texcoord);
    }
    else{
        hitPoint = vec3(up1.x, up1.y, intersectPlane(up1));
        rotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) *currentDragRotation;
        vec4 lutg = texture1D(lut, 0.);
        vec2 texcoord = vec2((rotatedHitPoint.x * rect.z - rect.x*rect.z), (rotatedHitPoint.y*rect.w*1.0-rect.y*rect.w)); 
        imageColor = texture2D(differenceImage, texcoord);
        imageColor = texture2D(image, texcoord);
        //imageColor = vec4(0., 1., 0., 1.);
    } 

    gl_FragColor = imageColor;
    
}
