#import <AppKit/AppKit.h>
#import <dispatch/dispatch.h>
#import <jawt_md.h>
#import <Metal/Metal.h>
#import <QuartzCore/CATransaction.h>
#import <QuartzCore/CAMetalLayer.h>

@interface JHVMetalHostBox : NSObject
@property(nonatomic, strong) CALayer *windowLayer;
@property(nonatomic, strong) CAMetalLayer *metalLayer;
@end

@implementation JHVMetalHostBox
@end

static void jhv_run_without_actions(void (^block)(void)) {
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    block();
    [CATransaction commit];
}

static void jhv_set_metal_layer_frame(CAMetalLayer *metalLayer, CGRect frame) {
    jhv_run_without_actions(^{
        if (!CGRectEqualToRect(metalLayer.frame, frame))
            metalLayer.frame = frame;

        CGSize drawableSize = CGSizeMake(frame.size.width * metalLayer.contentsScale, frame.size.height * metalLayer.contentsScale);
        if (!CGSizeEqualToSize(metalLayer.drawableSize, drawableSize))
            metalLayer.drawableSize = drawableSize;
    });
}

static CAMetalLayer *jhv_create_metal_layer(id<MTLDevice> device, CGFloat contentsScale, CGRect frame) {
    CAMetalLayer *metalLayer = [CAMetalLayer layer];
    metalLayer.device = device;
    metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
    metalLayer.framebufferOnly = NO;
    metalLayer.opaque = YES;
    metalLayer.contentsScale = contentsScale;
    // Without this the layer defaults to kCAGravityResize: when the layer is resized, Core Animation
    // stretches the *previous* frame to the new bounds until fresh content is drawn, so a programmatic
    // layout change (collapsing a panel) briefly shows a distorted frame even though the drawable and
    // the viewport both end up correct.
    //
    // Without this the layer defaults to kCAGravityResize, which stretches the previous frame to the
    // new bounds until fresh content is drawn -- a visibly distorted frame on any programmatic resize.
    //
    // No anchor makes a stale frame correct: the right placement depends on which edge moved. Centre
    // is the deliberate choice, because it keeps the sidebar collapse -- the frequent one -- clean,
    // the Sun being drawn about the canvas centre. It leaves a brief vertical shift when the timelines
    // panel is collapsed, which is a once-a-session action. The only fix without this trade is an
    // atomic native resize-and-render, and every route to that dispatches synchronously to the main
    // thread, which deadlocks against AppKit.
    metalLayer.contentsGravity = kCAGravityCenter;
    jhv_set_metal_layer_frame(metalLayer, frame);
    return metalLayer;
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
    if (windowLayer == nil)
        return 0.0;
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

            id<MTLDevice> device = MTLCreateSystemDefaultDevice();
            if (device == nil)
                return;

            JHVMetalHostBox *box = [JHVMetalHostBox new];
            CGFloat layerY = jhv_layer_y(windowLayer, y, height);
            CGFloat windowScale = jhv_window_scale(windowLayer);
            CGRect frame = CGRectMake(x, layerY, width, height);
            box.windowLayer = windowLayer;
            box.metalLayer = jhv_create_metal_layer(device, windowScale, frame);
            [windowLayer addSublayer:box.metalLayer];
            result = (__bridge_retained void *)box;
        }
    });
    return result;
}

static void jhv_apply_frame(JHVMetalHostBox *retainedBox, double x, double y, double width, double height) {
    @try {
        CGFloat layerY = jhv_layer_y(retainedBox.windowLayer, y, height);
        CGFloat windowScale = jhv_window_scale(retainedBox.windowLayer);
        if (retainedBox.metalLayer.contentsScale != windowScale)
            retainedBox.metalLayer.contentsScale = windowScale;
        CGRect frame = CGRectMake(x, layerY, width, height);
        jhv_set_metal_layer_frame(retainedBox.metalLayer, frame);
    } @finally {
        CFRelease((__bridge CFTypeRef)retainedBox);
    }
}

void jhv_metal_host_set_frame(void *boxPtr, double x, double y, double width, double height) {
    if (boxPtr == NULL)
        return;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    CFRetain((__bridge CFTypeRef)box);
    jhv_run_on_main_async(^{
        @autoreleasepool { jhv_apply_frame(box, x, y, width, height); }
    });
}

// Synchronous variant: the CAMetalLayer frame AND drawableSize are updated before returning, so a
// render issued immediately afterwards draws at the new resolution (no oblate frame, no flash).
// Used for programmatic resizes (collapsing the sidebar), not the frequent window-drag path.
void jhv_metal_host_set_frame_sync(void *boxPtr, double x, double y, double width, double height) {
    if (boxPtr == NULL)
        return;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    CFRetain((__bridge CFTypeRef)box);
    jhv_run_on_main_sync(^{
        @autoreleasepool { jhv_apply_frame(box, x, y, width, height); }
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
            [box.metalLayer removeFromSuperlayer];
        }
    });
}
