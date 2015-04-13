#version 110

uniform sampler2D image;
uniform float truncationValue;
uniform float isdifference;
uniform sampler2D differenceImage;
uniform vec4 pixelSizeWeighting;
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
    vec3 L = sphere.center - ray.origin; 
    float tca = dot(L, ray.direction);
    if (tca < 0.) {
        return t;
    }
    float dsq = dot(L, L) - tca * tca;
    float diff = sphere.radius * sphere.radius - dsq;
    if (diff < 0.) {
        return t;
    }
    t = tca - sqrt(diff);
    return t;      
}

float intersectPlane(in Ray ray, in Plane plane)
{   
    vec3 altnormal = (layerLocalRotation * vec4(plane.normal, 1.)).xyz;
    return -dot(ray.origin, altnormal) / dot(ray.direction, altnormal);
}

void intersect(in Ray ray, out float tSphere, out float tPlane)
{
        tSphere = intersectSphere(ray, sphere);
        tPlane = 1.;
       // tPlane = intersectPlane(ray, plane);
}

void main(void)
{  
    vec2 normalizedScreenpos = 2.*((gl_FragCoord.xy/viewport)-0.5);
    normalizedScreenpos.y = -normalizedScreenpos.y;

    vec4 up2 =  layerLocalRotation*(cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, 1., 1.));
    vec4 up1 =  layerLocalRotation*(cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.));
    vec3 direction = (up1 - up2).xyz;
    vec3 newdirection = -normalize(direction);
    vec3 origin = up1.xyz;    
    
    Ray ray = Ray(newdirection, origin);

    float tSphere = intersectSphere(ray, sphere);
  
    vec4 imageColor;
    //if(dot(direction.xyz, direction.xyz)>0.01){
    if(tSphere>0.){
    //if (dot(up1.xy, up1.xy)<1.){  
    //if (normalizedScreenpos.y<0.){  
/*        vec3 hitPoint = ray.origin + tSphere * ray.direction;
        vec3 rotatedHitPoint = (layerLocalRotation * vec4(hitPoint, 1)).xyz;

        if (rotatedHitPoint.z >= 0.0){
            vec2 texPos = (rotatedHitPoint.xy/2. + 0.5) *vec2(1.,1.) + vec2(1., 1.);
            float lutg = texture2D(lut, 0.);
        }
*/
            vec4 lutg = texture1D(lut, 0.);
            imageColor = texture2D(differenceImage, up1.xy);
        imageColor = texture2D(image, up1.xy);
        imageColor = vec4(0., 1., 0., 1.);
    }
    else{
        imageColor = vec4(1., 0., 0., 1.);
    }
    gl_FragColor = imageColor;
    
}
