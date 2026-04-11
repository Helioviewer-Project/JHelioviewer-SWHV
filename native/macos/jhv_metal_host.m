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
            CGFloat windowScale = windowLayer.contentsScale;
            if (windowScale <= 0.0)
                windowScale = NSScreen.mainScreen.backingScaleFactor;
            if (windowScale <= 0.0)
                windowScale = 1.0;
            CGRect frame = CGRectMake(x, layerY, width, height);
            box.windowLayer = windowLayer;
            box.metalLayer = jhv_create_metal_layer(device, windowScale, frame);
            [windowLayer addSublayer:box.metalLayer];
            result = (__bridge_retained void *)box;
        }
    });
    return result;
}

void jhv_metal_host_set_frame(void *boxPtr, double x, double y, double width, double height) {
    if (boxPtr == NULL)
        return;

    JHVMetalHostBox *box = (__bridge JHVMetalHostBox *)boxPtr;
    jhv_run_on_main_async(^{
        @autoreleasepool {
            CGFloat layerY = jhv_layer_y(box.windowLayer, y, height);
            CGRect frame = CGRectMake(x, layerY, width, height);
            jhv_set_metal_layer_frame(box.metalLayer, frame);
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
            [box.metalLayer removeFromSuperlayer];
        }
    });
}
