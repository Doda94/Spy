import SwiftUI
import ComposeApp

/// Hosts the shared Compose UI. Native iOS 26 chrome (tab bar, glass buttons) arrives with the
/// Dictionaries and Settings tabs; the in-game screens are immersive and fully Compose.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ComposeView()
            .overlay {
                // The app-switcher snapshot must never show a role or the word (decisions.md §4,
                // motion.md §11): cover the UI whenever the scene isn't active. Solid, because a thin
                // material still lets the large display text through.
                if scenePhase != .active {
                    (colorScheme == .dark ? Color(red: 0x12 / 255, green: 0x11 / 255, blue: 0x10 / 255)
                                          : Color(red: 0xF6 / 255, green: 0xF2 / 255, blue: 0xEA / 255))
                        .ignoresSafeArea()
                }
            }
    }
}
