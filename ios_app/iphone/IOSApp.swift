import SwiftUI
import Combine
import shared

@main
struct IOSApp: App {
    
    @State private var vm = AppVm()
    
    @Environment(\.scenePhase) private var scenePhase
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var myInAppNotificationDelegate = MyInAppNotificationDelegate()
    
    private let scheduledNotificationsDataPublisher: AnyPublisher<NSArray, Never> =
    Utils_kmpKt.scheduledNotificationsDataFlow.toPublisher()
    
    private let keepScreenOnDataPublisher: AnyPublisher<KotlinBoolean, Never> =
    Utils_kmpKt.keepScreenOnStateFlow.toPublisher()
    
    private let batteryManager = BatteryManager() // Keep the object
    
    init() {
        Utils_kmp_iosKt.doInitKmpIos()
    }
    
    var body: some Scene {
        
        WindowGroup {
            
            // todo refactor
            VMView(vm: vm) { state in
                
                if let backupMessage = state.backupMessage {
                    BackupMessageView(message: backupMessage)
                } else if state.isAppReady {
                    
                    MainScreen()
                        .attachFs()
                        .attachTimetoAlert()
                        .attachAutoBackupIos()
                        .attachNativeSheet()
                        .statusBar(hidden: true)
                        .onReceive(scheduledNotificationsDataPublisher) {
                            let center = UNUserNotificationCenter.current()
                            center.removeAllPendingNotificationRequests()
                            let dataItems = $0 as! [ScheduledNotificationData]
                            dataItems.forEach { data in
                                schedulePush(data: data)
                            }
                        }
                        .onReceive(keepScreenOnDataPublisher) { keepScreenOn in
                            UIApplication.shared.isIdleTimerDisabled = (keepScreenOn == true)
                        }
                        .onAppear {
                            /// Use together
                            UNUserNotificationCenter
                                .current()
                                .requestAuthorization(options: [.badge, .sound, .alert]) { isGranted, _ in
                                    if isGranted {
                                        // Without delay the first event does not handled. 50mls enough.
                                        vm.onNotificationsPermissionReady(delayMls: Int64(500))
                                    }
                                }
                            UNUserNotificationCenter.current().delegate = myInAppNotificationDelegate
                            ///
                        }
                }
            }
        }
        .onChange(of: scenePhase) { phase in
            // Remove notifications and badges
            // https://stackoverflow.com/a/41487410
            // https://betterprogramming.pub/swiftui-tips-detecting-a-swiftui-apps-active-inactive-and-background-state-a5ff8acf5db1
            if phase == .active {
                UNUserNotificationCenter.current().removeAllDeliveredNotifications()
                UIApplication.shared.applicationIconBadgeNumber = 0
            }
        }
    }
}

private struct BackupMessageView: View {
    
    let message: String
    
    var body: some View {
        VStack {
            HStack {
                Spacer()
            }
            Spacer()
            Text(message)
                .foregroundColor(c.white)
            Spacer()
        }
        .background(c.black)
    }
}
