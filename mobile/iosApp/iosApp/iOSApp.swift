import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // spijun://dictionary?d=… opens the import preview; Kotlin ignores any other link.
                .onOpenURL { url in IncomingLinks.shared.deliver(url: url.absoluteString) }
        }
    }
}
