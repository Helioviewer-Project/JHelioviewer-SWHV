#import <AppKit/AppKit.h>
#import <dispatch/dispatch.h>
#import <jawt_md.h>
#import <Metal/Metal.h>
#import <QuartzCore/CAMetalLayer.h>

@interface JHVMetalHostBox : NSObject
@property(nonatomic, strong) id<JAWT_SurfaceLayers> surfaceLayers;
@property(nonatomic, strong) CALayer *rootLayer;
@property(nonatomic, strong) CAMetalLayer *metalLayer;
@end

@implementation JHVMetalHostBox
@end

static CAMetalLayer *jhv_create_metal_layer(CGFloat contentsScale, CGSize size) {
    CAMetalLayer *metalLayer = [CAMetalLayer layer];
    metalLayer.opaque = YES;
    metalLayer.contentsScale = contentsScale;
    metalLayer.contentsGravity = kCAGravityCenter;
    metalLayer.frame = CGRectMake(0.0, 0.0, size.width, size.height);
    metalLayer.autoresizingMask = kCALayerWidthSizable | kCALayerHeightSizable;
    return metalLayer;
}

static CALayer *jhv_create_root_layer(CAMetalLayer *metalLayer, CGRect frame) {
    CALayer *rootLayer = [CALayer layer];
    rootLayer.masksToBounds = YES;
    rootLayer.frame = frame;
    [rootLayer addSublayer:metalLayer];
    return rootLayer;
}

static void jhv_run_on_main_sync(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
        return;
    }

    dispatch_sync(dispatch_get_main_queue(), block);
}

static void jhv_run_on_main_async(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
        return;
    }

    dispatch_async(dispatch_get_main_queue(), block);
}

static id<JAWT_SurfaceLayers> jhv_surface_layers(void *surfaceLayersPtr) {
    if (surfaceLayersPtr == NULL)
        return nil;

    id surfaceLayers = (__bridge id)surfaceLayersPtr;
    if (![surfaceLayers conformsToProtocol:@protocol(JAWT_SurfaceLayers)])
        return nil;

    return (id<JAWT_SurfaceLayers>)surfaceLayers;
}

static CGFloat jhv_layer_y(CALayer *windowLayer, double y, double height) {
    return windowLayer.geometryFlipped ? y : (windowLayer.bounds.size.height - y - height);
}

static CGFloat jhv_window_scale(CALayer *windowLayer) {
    CGFloat windowScale = windowLayer.contentsScale;
    if (windowScale <= 0.0)
        windowScale = NSScreen.mainScreen.backingScaleFactor;
    if (windowScale <= 0.0)
        windowScale = 1.0;
    return windowScale;
}

const char *jhv_metal_device_info(void) {
    static char info[256];

    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (device == nil) {
            snprintf(info, sizeof(info), "available=false reason=no default Metal device");
            return info;
        }

        const char *name = device.name.UTF8String;
        snprintf(info, sizeof(info),
                 "MTLGPUFamilyMac2=%s name=\"%s\"",
                 [device supportsFamily:MTLGPUFamilyMac2] ? "true" : "false",
                 name != NULL ? name : "");
        return info;
    }
}

void *jhv_metal_host_create(void *surfaceLayersPtr, double x, double y, double width, double height) {
    __block void *result = NULL;
    jhv_run_on_main_sync(^{
        @autoreleasepool {
            id<JAWT_SurfaceLayers> surfaceLayers = jhv_surface_layers(surfaceLayersPtr);
            if (surfaceLayers == nil)
                return;

            CALayer *windowLayer = surfaceLayers.windowLayer;
            if (windowLayer == nil)
                return;

            JHVMetalHostBox *box = [JHVMetalHostBox new];
            CGFloat windowScale = jhv_window_scale(windowLayer);
            CGRect frame = CGRectMake(x, jhv_layer_y(windowLayer, y, height), width, height);
            box.surfaceLayers = surfaceLayers;
            box.metalLayer = jhv_create_metal_layer(windowScale, frame.size);
            box.rootLayer = jhv_create_root_layer(box.metalLayer, frame);
            surfaceLayers.layer = box.rootLayer;
            result = (__bridge_retained void *)box;
        }
    });
    return result;
}

void jhv_metal_host_set_scale(void *boxPtr, double scale) {
    if (boxPtr == NULL)
        return;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    jhv_run_on_main_sync(^{
        @autoreleasepool {
            if (box.metalLayer.contentsScale != scale)
                box.metalLayer.contentsScale = scale;
        }
    });
}

void jhv_metal_host_set_visible(void *boxPtr, int visible) {
    if (boxPtr == NULL)
        return;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    jhv_run_on_main_async(^{
        @autoreleasepool {
            box.metalLayer.hidden = visible == 0;
        }
    });
}

void *jhv_metal_host_get_layer(void *boxPtr) {
    if (boxPtr == NULL)
        return NULL;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    return (__bridge void *)box.metalLayer;
}

void jhv_metal_host_destroy(void *boxPtr) {
    if (boxPtr == NULL)
        return;

    jhv_run_on_main_sync(^{
        @autoreleasepool {
            JHVMetalHostBox *box = (__bridge_transfer JHVMetalHostBox *)boxPtr;
            if (box.surfaceLayers.layer == box.rootLayer)
                box.surfaceLayers.layer = nil;
        }
    });
}
