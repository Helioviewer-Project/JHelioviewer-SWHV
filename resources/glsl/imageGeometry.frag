
void clipNormalizedCoord(const vec2 coord) {
    if (coord.x < display.slit.x || coord.y < 0. || coord.x > display.slit.y || coord.y > 1.)
        discard;
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
// horizontal aspect scaling, move the origin to the lower-left, and discard outside it.
vec2 getNormalizedMapPos(void) {
    vec2 pos = getViewPosition();
    pos = vec2(screen.iaspect * pos.x, pos.y) + .5;
    clipNormalizedCoord(pos);
    return pos;
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
    return dt * (0.01367 - 0.339 * sinLat2 - 0.485 * sinLat2 * sinLat2); // 2.879 urad/s - 14.1844 deg/86400s (not fully right: 1st SI, 2nd TDB)
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

vec3 differentialInSourceFrame(const Image img, const vec3 point) {
    vec3 world = rotate_vector_inverse(img.sourceViewQuat, point);
    return rotate_vector(img.sourceViewQuat, differential(img.deltaT, world));
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

// Homogeneous direction -> native east/north/zenith. WCSLIB prepares this frame.
vec3 nativeCoordinates(const vec3 direction, const Image img) {
    return vec3(dot(img.nativeX.xyz, direction), dot(img.nativeY.xyz, direction), dot(img.nativeZ.xyz, direction));
}

float zpnRadial(const float eta, const float[6] PV) {
    return (((((PV[5] * eta + PV[4]) * eta + PV[3]) * eta + PV[2]) * eta + PV[1]) * eta + PV[0]);
}

vec2 nativeToWcsPlane(const vec3 q, const Image img, const float[6] PV) {
    float scale = img.planeUnitsPerRadian;
    if (img.projectionCode == WCS_PROJECTION_TAN) {
        if (q.z <= 0.)
            discard;
        return scale * q.xy / q.z;
    }
    if (img.projectionCode == WCS_PROJECTION_AZP) {
        float lengthQ = length(q);
        float mu = PV[1];
        float denominator = mu * lengthQ + q.z - q.y * img.nativeX.w;
        if (denominator * (mu + 1.) <= 0.)
            discard;
        if (abs(mu) > 1. && q.z < -lengthQ / mu)
            discard;
        return scale * (mu + 1.) * vec2(q.x, q.y * img.nativeY.w) / denominator;
    }
    float radius = length(q.xy);
    vec2 direction = radius == 0. ? (q.z >= 0. ? img.nativePoles.xy : img.nativePoles.zw) : q.xy / radius;
    if (img.projectionCode == WCS_PROJECTION_CAR || img.projectionCode == WCS_PROJECTION_CEA) {
        // Resolve zero longitude explicitly, including signed zero on SwiftShader.
        float longitude = direction.x == 0. && direction.y <= 0. ? 0. : atan(direction.x, -direction.y);
        if (longitude == PI)
            longitude = -PI;
        float latitude = img.projectionCode == WCS_PROJECTION_CAR
                ? atan(q.z, radius) : q.z / (length(q) * PV[1]);
        return scale * vec2(longitude, latitude);
    }
    float eta = atan(radius, q.z);
    float radial = eta;
    if (img.projectionCode == WCS_PROJECTION_ZPN) {
        if (eta > img.nativeZ.w)
            discard;
        radial = zpnRadial(eta, PV);
    }
    // Nonzero ZPN P0 is a radius, including at the north pole.
    return scale * radial * direction;
}

vec2 wcsPlaneToUnclampedTexcoord(const vec2 plane, const Image img) {
    vec3 p = vec3(plane, 1.);
    return vec2(dot(img.sourceX.xyz, p), dot(img.sourceY.xyz, p));
}

vec2 wcsPlaneToTexcoord(const vec2 plane, const Image img) {
    vec2 texcoord = wcsPlaneToUnclampedTexcoord(plane, img);
    clipNormalizedCoord(texcoord);
    return texcoord;
}

vec2 observerDirectionToTexcoord(const vec3 direction, const Image img, const float[6] PV) {
    return wcsPlaneToTexcoord(nativeToWcsPlane(nativeCoordinates(direction, img), img, PV), img);
}

vec2 observerToTexcoord(const vec3 world, const Image img, const float[6] PV) {
    return observerDirectionToTexcoord(vec3(world.xy, img.observerDistance - world.z), img, PV);
}

vec2 longitudeTurnRange(const float coordinate, const float displacement, const vec2 bounds) {
    vec2 turns = (bounds - coordinate) / displacement;
    return vec2(ceil(min(turns.x, turns.y)), floor(max(turns.x, turns.y)));
}

vec2 wcsPlaneToSurfaceTexcoord(const vec2 plane, const Image img) {
    vec2 texcoord = wcsPlaneToUnclampedTexcoord(plane, img);
    // A longitude turn follows the effective affine transform, including user
    // rotation. It need not equal one image width or follow texture X.
    vec2 displacement = TWOPI * img.planeUnitsPerRadian * vec2(img.sourceX.x, img.sourceY.x);
    vec2 turns;
    if (displacement.x == 0.) {
        if (texcoord.x < display.slit.x || texcoord.x > display.slit.y)
            discard;
        turns = longitudeTurnRange(texcoord.y, displacement.y, vec2(0., 1.));
    } else {
        turns = longitudeTurnRange(texcoord.x, displacement.x, display.slit);
        if (displacement.y != 0.) {
            vec2 vertical = longitudeTurnRange(texcoord.y, displacement.y, vec2(0., 1.));
            turns = vec2(max(turns.x, vertical.x), min(turns.y, vertical.y));
        } else if (texcoord.y < 0. || texcoord.y > 1.) {
            discard;
        }
    }
    if (turns.x > turns.y)
        discard;
    // Keep the principal longitude when available, otherwise the nearest turn.
    texcoord += clamp(0., turns.x, turns.y) * displacement;
    clipNormalizedCoord(texcoord);
    return texcoord;
}

bool isSurfaceMap(const Image img) {
    return img.projectionCode == WCS_PROJECTION_CAR
        || img.projectionCode == WCS_PROJECTION_CEA;
}

vec2 sampleSurfaceMapTexcoord(const vec3 world, const Image img, const float[6] PV) {
    vec2 plane = nativeToWcsPlane(nativeCoordinates(world, img), img, PV);
    return wcsPlaneToSurfaceTexcoord(plane, img);
}

bool observerRayToWorld(const vec3 ray, const float observerDistance, out vec3 world) {
    float a = dot(ray, ray);
    float b = observerDistance * ray.z;
    // Avoid subtracting terms of order distance^2; trig approximations need not give a unit ray.
    vec2 impact = observerDistance * ray.xy;
    float discriminant = a - dot(impact, impact);
    if (discriminant < 0.) {
        world = vec3(0.);
        return false;
    }

    float root = sqrt(discriminant);
    float signedRoot = -b - root > 0. ? -root : root;
    float t = (-b + signedRoot) / a;
    if (t <= 0.) {
        world = vec3(0.);
        return false;
    }

    // Expand Z to avoid cancelling the observer distance against t * ray.z.
    world = vec3(t * ray.xy, (dot(impact, ray.xy) + signedRoot * ray.z) / a);
    return true;
}

void clipSectorOpening(const float theta, const vec2 sector) {
    if (sector.y <= 0.)
        return;

    float delta = abs(theta - sector.x);
    float angularDistance = min(delta, TWOPI - delta);
    if (angularDistance < sector.y)
        discard;
}

void clipSectors(const vec2 point) {
    if (display.metadataSector.y <= 0. && display.userSector.y <= 0.)
        return;

    float theta = atan(point.y, point.x);
    clipSectorOpening(theta, display.metadataSector);
    clipSectorOpening(theta, display.userSector);
}

void clipPlanarMasks(const vec2 point) {
    clipSectors(point);

    float radial2 = dot(point, point);
    float minRadius2 = display.radii.x * display.radii.x;
    float maxRadius2 = display.radii.y * display.radii.y;
    if (radial2 > maxRadius2 || radial2 < minRadius2)
        discard;

    if (display.cutOff.z >= 0.) {
        float flatDist = abs(dot(point, display.cutOff.xy));
        vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
        float flatDistAlt = abs(dot(point, cutOffAlt));
        if (flatDist > display.cutOff.z || flatDistAlt > display.cutOff.z)
            discard;
    }
}

vec2 sampleHpcTexcoord(const Image img, const vec3 ray, const vec2 hpcXY, const float[6] PV, out float enhancementFactor) {
    enhancementFactor = 1.;
    float observerDistance = img.observerDistance;

    vec3 world;
    bool onDisk = observerRayToWorld(ray, observerDistance, world);
    if (isSurfaceMap(img)) {
        if (!onDisk)
            discard;
        return sampleSurfaceMapTexcoord(rotate_vector_inverse(img.sourceViewQuat, world), img, PV);
    }
    if (onDisk && img.deltaT != 0.) {
        vec3 rotatedWorld = differentialInSourceFrame(img, world);
        return observerToTexcoord(rotatedWorld, img, PV);
    }
    if (!onDisk)
        enhancementFactor = max(1., length(hpcXY));

    // Observer rays use -Z at helioprojective (0,0); WCS directions use +Z with the same X/Y.
    return observerDirectionToTexcoord(vec3(ray.xy, -ray.z), img, PV);
}

vec4 sampleWarpedHpcColor(const vec2 hpcXY) {
    vec3 ray = vec3(hpcXY, -images[0].observerDistance);
    clipPlanarMasks(hpcXY);
    float enhancementFactor;
    vec2 texCoord = sampleHpcTexcoord(images[0], ray, hpcXY, pv0, enhancementFactor);
    if (display.isDiff == NODIFFERENCE)
        return getColor(texCoord, texCoord, enhancementFactor);

    vec3 diffRay = vec3(hpcXY, -images[1].observerDistance);
    float diffEnhancementFactor;
    vec2 diffTexCoord = sampleHpcTexcoord(images[1], diffRay, hpcXY, pv1, diffEnhancementFactor);
    return getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
}
