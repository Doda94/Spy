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
    var body: some View {
        ComposeView()
    }
}
