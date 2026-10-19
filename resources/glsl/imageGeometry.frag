
bool insideImage(const vec2 coord) {
    return coord.x >= display.slit.x && coord.x <= display.slit.y && coord.y >= 0. && coord.y <= 1.;
}

bool insideMap(const vec2 coord) {
    return coord.x >= 0. && coord.x <= 1. && coord.y >= 0. && coord.y <= 1.;
}

// Convert normalized screen coordinates to the view-aligned plane in scene units.
// The projection is orthographic, so xy is independent of clip-space z and needs no perspective divide.
vec2 getViewPosition(void) {
    return (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;
}

float getDepth(const float viewZ) {
    return 0.5 * (1. + viewZ / screen.inverseMVP[2][2]);
}

// Map the centered view plane to the [0, 1] map domain: remove the viewport's
// horizontal aspect scaling and move the origin to the lower-left.
vec2 getNormalizedMapPos(void) {
    vec2 pos = getViewPosition();
    return vec2(screen.iaspect * pos.x, pos.y) + .5;
}

// Convert a normalized warp radius back to radial distance in solar radii.
// The disk is linear; only distances beyond the limb use Box-Cox scaling.
float unwarpRadius(float normalizedRadius) {
    float outerRadius = screen.mapBounds.w;
    float limbPosition = 1. / outerRadius;
    if (outerRadius <= 1. || normalizedRadius <= limbPosition)
        return normalizedRadius / limbPosition;

    float u = (normalizedRadius - limbPosition) / (1. - limbPosition);
    float lambda = screen.lambda;
    return lambda == 0.
            ? pow(outerRadius, u)
            : pow(1. + u * (pow(outerRadius, lambda) - 1.), 1. / lambda);
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

// Differential solar rotation.
float differentialRotation(const float dt, const float sinLatitude) {
    float sinLat2 = sinLatitude * sinLatitude;
    // Snodgrass, Table 1 Magnetic - http://articles.adsabs.harvard.edu/pdf/1990ApJ...351..309S
    return dt * (0.01367034236 - 0.339 * sinLat2 - 0.485 * sinLat2 * sinLat2); // 2.879 urad/s - 14.1844 deg/86400s (not fully right: 1st SI, 2nd TDB)
}

vec3 differential(const float dt, const vec3 v) {
    float delta = differentialRotation(dt, v.y);
    float sinDelta = sin(delta);
    float cosDelta = cos(delta);
    return vec3(
        v.x * cosDelta - v.z * sinDelta,
        v.y,
        v.z * cosDelta + v.x * sinDelta);
}

vec3 differentialInImageFrame(const Image img, const vec3 point) {
    if (img.deltaT == 0.)
        return point;
    vec3 world = rotate_vector_inverse(img.solarToImage, point);
    return rotate_vector(img.solarToImage, differential(img.deltaT, world));
}

// SwiftShader needs accurate quadrant reduction; hardware backends use their intrinsics.
vec2 sinCos(const float angle) {
#ifdef JHV_SWIFTSHADER
    // Evaluate a quadrant residual, then apply the low part of pi/2 as a rotation.
    float quadrant = floor(angle * (2. / PI) + .5);
    float x = angle - quadrant * 1.5703125;
    float x2 = x * x;
    float s = x * (1. + x2 * (-1. / 6. + x2 * (1. / 120. + x2 * (-1. / 5040. + x2 * (1. / 362880. - x2 / 39916800.)))));
    float c = 1. + x2 * (-.5 + x2 * (1. / 24. + x2 * (-1. / 720. + x2 * (1. / 40320. - x2 / 3628800.))));
    // Apply the low part as a small rotation. This also prevents shader fast-math
    // from folding the split constant back into one rounded multiple of pi/2.
    float delta = quadrant * 0.00048382679489661923;
    float d2 = delta * delta;
    float sd = delta * (1. - d2 / 6.);
    float cd = 1. - d2 / 2.;
    vec2 pair = vec2(s * cd - c * sd, c * cd + s * sd);
    float q = mod(quadrant, 4.);
    if (q == 0.) return pair;
    if (q == 1.) return vec2(pair.y, -pair.x);
    if (q == 2.) return -pair;
    return vec2(-pair.y, pair.x);
#else
    return vec2(sin(angle), cos(angle));
#endif
}

vec3 helioprojectiveToObserverRay(const vec2 angles) {
    vec2 lon = sinCos(angles.x);
    vec2 lat = sinCos(angles.y);
    return vec3(lon.x * lat.y, lat.x, -lon.y * lat.y);
}

float zpnRadial(const float eta, const Image img) {
    return (((((img.pv5 * eta + img.pv4) * eta + img.pv3) * eta + img.pv2) * eta + img.pv1) * eta + img.pv0);
}

vec2 wcsPlaneToTexcoord(const vec2 plane, const Image img) {
    return plane.x * img.imageFromPlaneX + plane.y * img.imageFromPlaneY + img.imageOffset;
}

vec2 longitudeTurnRange(const float coordinate, const float displacement, const vec2 bounds) {
    vec2 turns = (bounds - coordinate) / displacement;
    return vec2(ceil(min(turns.x, turns.y)), floor(max(turns.x, turns.y)));
}

bool wrapSurfaceTexcoord(const Image img, inout vec2 texcoord) {
    // A longitude turn follows the image affine transform, including user rotation.
    vec2 displacement = TWOPI * img.imageFromPlaneX;
    vec2 turns;
    if (displacement.x == 0.) {
        if (texcoord.x < display.slit.x || texcoord.x > display.slit.y)
            return false;
        turns = longitudeTurnRange(texcoord.y, displacement.y, vec2(0., 1.));
    } else {
        turns = longitudeTurnRange(texcoord.x, displacement.x, display.slit);
        if (displacement.y != 0.) {
            vec2 vertical = longitudeTurnRange(texcoord.y, displacement.y, vec2(0., 1.));
            turns = vec2(max(turns.x, vertical.x), min(turns.y, vertical.y));
        } else if (texcoord.y < 0. || texcoord.y > 1.) {
            return false;
        }
    }
    if (turns.x > turns.y)
        return false;
    texcoord += clamp(0., turns.x, turns.y) * displacement;
    return true;
}

const float SURFACE_RADIUS2 = 1. + 1e-5;

bool isSurfaceMap(const Image img) {
    return img.projectionCode == WCS_PROJECTION_CAR || img.projectionCode == WCS_PROJECTION_CEA;
}

bool imageTexcoord(const vec3 direction, const Image img, out vec2 texcoord) {
    vec2 plane;
    if (img.projectionCode == ORTHOGRAPHIC_TAN) {
        plane = direction.xy;
    } else {
        vec3 q = img.projectionFromRay * direction;
        if (img.projectionCode == WCS_PROJECTION_TAN) {
            if (q.z <= 0.)
                return false;
            plane = q.xy / q.z;
        } else if (img.projectionCode == WCS_PROJECTION_AZP) {
            float lengthQ = length(q);
            float mu = img.pv1;
            float denominator = mu * lengthQ + q.z - q.y * img.azpTanTilt;
            if (denominator * (mu + 1.) <= 0.)
                return false;
            if (abs(mu) > 1. && q.z < -lengthQ / mu)
                return false;
            plane = (mu + 1.) * vec2(q.x, q.y * img.azpSecTilt) / denominator;
        } else {
            float radius = length(q.xy);
            vec2 radialDirection = radius == 0. ? (q.z >= 0. ? img.northPole : img.southPole) : q.xy / radius;
            if (img.projectionCode == WCS_PROJECTION_CAR || img.projectionCode == WCS_PROJECTION_CEA) {
                // Maps cover the sphere only; off-limb points lie outside it.
                if (dot(q, q) > SURFACE_RADIUS2)
                    return false;
                // Resolve zero longitude explicitly, including signed zero on SwiftShader.
                float longitude = radialDirection.x == 0. && radialDirection.y <= 0. ? 0. : atan(radialDirection.x, -radialDirection.y);
                if (longitude == PI)
                    longitude = -PI;
                float latitude = img.projectionCode == WCS_PROJECTION_CAR
                        ? atan(q.z, radius) : q.z / (length(q) * img.pv1);
                plane = vec2(longitude, latitude);
                texcoord = wcsPlaneToTexcoord(plane, img);
                return wrapSurfaceTexcoord(img, texcoord) && insideImage(texcoord);
            }
            float eta = atan(radius, q.z);
            float radial = eta;
            if (img.projectionCode == WCS_PROJECTION_ZPN) {
                if (eta > img.zpnMaxAngle)
                    return false;
                radial = zpnRadial(eta, img);
            }
            // Nonzero ZPN P0 is a radius, including at the north pole.
            plane = radial * radialDirection;
        }
    }
    texcoord = wcsPlaneToTexcoord(plane, img);
    return insideImage(texcoord);
}

bool observerRayToWorld(const vec3 ray, const float observerDistance, out vec3 world) {
    float a = dot(ray, ray);
    float b = observerDistance * ray.z;
    // Avoid subtracting terms of order distance^2; trig approximations need not give a unit ray.
    vec2 impact = observerDistance * ray.xy;
    float discriminant = a - dot(impact, impact);
    if (discriminant < 0.)
        return false;

    float root = sqrt(discriminant);
    float signedRoot = -b - root > 0. ? -root : root;
    float t = (-b + signedRoot) / a;
    if (t <= 0.)
        return false;

    // Expand Z to avoid cancelling the observer distance against t * ray.z.
    world = vec3(t * ray.xy, (dot(impact, ray.xy) + signedRoot * ray.z) / a);
    return true;
}

bool insideSector(const float theta, const vec2 sector) {
    if (sector.y <= 0.)
        return false;
    float delta = abs(theta - sector.x);
    return sector.y >= PI || min(delta, TWOPI - delta) < sector.y;
}

bool insideSectorMasks(const vec2 point) {
    if (display.metadataSector.y > 0. || display.userSector.y > 0.) {
        if (dot(point, point) == 0.)
            return false;
        float theta = atan(point.y, point.x);
        if (insideSector(theta, display.metadataSector) || insideSector(theta, display.userSector))
            return false;
    }
    return true;
}

bool insidePlanarMasks(const vec2 point) {
    float radial2 = dot(point, point);
    if (radial2 > display.radii.y * display.radii.y || radial2 < display.radii.x * display.radii.x)
        return false;
    if (!insideSectorMasks(point))
        return false;
    vec2 perpendicular = vec2(-display.cutOff.y, display.cutOff.x);
    return abs(dot(point, display.cutOff.xy)) <= display.cutOff.z && abs(dot(point, perpendicular)) <= display.cutOff.z;
}

// Every image projects a prepared-image-frame point through (p.xy, projectionOriginZ - p.z).
vec3 imageDirection(const Image img, const vec3 point) {
    return vec3(point.xy, img.projectionOriginZ - point.z);
}

// Sphere points below the source horizon have no image data.
bool sampleImagePoint(const Image img, const vec3 point, out vec2 texcoord) {
    return point.z >= img.minSurfaceZ && imageTexcoord(imageDirection(img, point), img, texcoord);
}

bool hpcMask(const vec3 ray, const float observerDistance) {
    // No forward plane: infinite mask radius, sectors by ray direction.
    if (ray.z >= 0.)
        return isinf(display.radii.y) && isinf(display.cutOff.z) && insideSectorMasks(ray.xy);
    return insidePlanarMasks(-observerDistance * ray.xy / ray.z);
}

bool hpcTexcoord(const Image img, const vec3 ray, out vec2 texcoord, out float enhancementFactor) {
    enhancementFactor = 1.;
    if (!hpcMask(ray, img.observerDistance))
        return false;
    // Observer images need a sphere point only for time correction or off-limb enhancement.
    vec3 point;
    if ((isSurfaceMap(img) || img.deltaT != 0. || display.enhanced != 0.)
            && observerRayToWorld(ray, img.observerDistance, point))
        return sampleImagePoint(img, differentialInImageFrame(img, point), texcoord);
    if (isSurfaceMap(img))
        return false; // A surface map has no image beyond the sphere.
    // Without a surface correction, every point along an observer ray projects alike.
    // Radial enhancement follows the Sun-plane radius, which rear rays do not have.
    if (ray.z < 0.)
        enhancementFactor = length(img.observerDistance * ray.xy / ray.z);
    // Direct observer rays need no point construction or subtraction of observer distances.
    return imageTexcoord(vec3(ray.xy, -ray.z), img, texcoord);
}

// Rendering owns fragment rejection; coordinate helpers only report validity.
void renderHpc(const vec3 ray, const vec3 diffRay) {
    vec2 texCoord;
    float enhancementFactor;
    if (!hpcTexcoord(images[0], ray, texCoord, enhancementFactor))
        discard;
    vec2 diffTexCoord = texCoord;
    if (display.isDiff != NODIFFERENCE) {
        float diffEnhancementFactor;
        if (!hpcTexcoord(images[1], diffRay, diffTexCoord, diffEnhancementFactor))
            discard;
        enhancementFactor = max(enhancementFactor, diffEnhancementFactor);
    }
    renderImage(texCoord, diffTexCoord, enhancementFactor);
}
