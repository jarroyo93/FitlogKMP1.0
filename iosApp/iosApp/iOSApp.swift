import SwiftUI
import FirebaseCore // o la librería que uses para Firebase/KMP

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
