#import "EmpressaPosPlugin.h"
#if __has_include(<horizon_pos/horizon_pos-Swift.h>)
#import <horizon_pos/horizon_pos-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "horizon_pos-Swift.h"
#endif

@implementation EmpressaPosPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftEmpressaPosPlugin registerWithRegistrar:registrar];
}
@end
