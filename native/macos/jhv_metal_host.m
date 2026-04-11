#import <AppKit/AppKit.h>
#import <dispatch/dispatch.h>
#import <Metal/Metal.h>
#import <QuartzCore/CATransaction.h>
#import <QuartzCore/CAMetalLayer.h>

@interface JHVMetalHostBox : NSObject
@property(nonatomic, strong) NSView *childView;
@property(nonatomic, strong) NSView *hostView;
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

static CAMetalLayer *jhv_create_metal_layer(id<MTLDevice> device, CGRect frame) {
    CAMetalLayer *metalLayer = [CAMetalLayer layer];
    metalLayer.device = device;
    metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
    metalLayer.framebufferOnly = NO;
    metalLayer.opaque = YES;
    metalLayer.contentsScale = NSScreen.mainScreen.backingScaleFactor ?: 1.0;
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

static NSView *jhv_host_view(void *hostPtr) {
    if (hostPtr == NULL)
        return nil;

    id host = (__bridge id)hostPtr;
    if (![host isKindOfClass:[NSView class]])
        return nil;

    return (NSView *)host;
}

static CGFloat jhv_host_y(NSView *hostView, double y, double height) {
    if (hostView == nil)
        return 0.0;
    return hostView.isFlipped ? y : (hostView.bounds.size.height - y - height);
}

void *jhv_metal_host_create(void *hostPtr, double x, double y, double width, double height) {
    __block void *result = NULL;
    jhv_run_on_main_sync(^{
        @autoreleasepool {
            NSView *hostView = jhv_host_view(hostPtr);
            if (hostView == nil)
                return;

            id<MTLDevice> device = MTLCreateSystemDefaultDevice();
            if (device == nil)
                return;

            CGFloat hostY = jhv_host_y(hostView, y, height);
            JHVMetalHostBox *box = [JHVMetalHostBox new];
            box.hostView = hostView;
            CAMetalLayer *metalLayer;

            CALayer *hostLayer = hostView.layer;
            if (hostLayer != nil) {
                metalLayer = jhv_create_metal_layer(device, CGRectMake(x, hostY, width, height));
                [hostLayer addSublayer:metalLayer];
            } else {
                NSView *childView = [[NSView alloc] initWithFrame:NSMakeRect(x, hostY, width, height)];
                childView.wantsLayer = YES;
                metalLayer = jhv_create_metal_layer(device, CGRectMake(0.0, 0.0, width, height));
                childView.layer = metalLayer;

                [hostView addSubview:childView positioned:NSWindowAbove relativeTo:nil];
                [hostView setNeedsDisplay:YES];
                [childView setNeedsDisplay:YES];
                [hostView.window.contentView setNeedsDisplay:YES];

                box.childView = childView;
            }
            box.metalLayer = metalLayer;
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
            CGFloat hostY = jhv_host_y(box.hostView, y, height);
            if (box.childView == nil) {
                jhv_set_metal_layer_frame(box.metalLayer, CGRectMake(x, hostY, width, height));
                return;
            }

            CGRect childFrame = CGRectMake(x, hostY, width, height);
            jhv_run_without_actions(^{
                if (!CGRectEqualToRect(box.childView.frame, childFrame))
                    box.childView.frame = childFrame;
            });
            jhv_set_metal_layer_frame(box.metalLayer, CGRectMake(0.0, 0.0, width, height));
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
            if (box.childView != nil) {
                [box.childView removeFromSuperview];
                return;
            }
            [box.metalLayer removeFromSuperlayer];
        }
    });
}
