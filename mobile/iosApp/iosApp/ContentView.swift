import SwiftUI
import ComposeApp

/// The tabs, stacks and game cover as Kotlin's `IosShell` reports them.
final class ShellModel: ObservableObject {
    @Published private(set) var state: ShellState?

    init() {
        IosShell.shared.observe { [weak self] state in self?.state = state }
    }
}

/// One shared Compose screen. Its top bar (back, title, actions) arrives through `onBarChange`.
final class ScreenModel: ObservableObject {
    let screen: IosScreen
    @Published private(set) var bar: BarShape?

    init(_ screen: IosScreen) {
        self.screen = screen
        screen.onBarChange = { [weak self] bar in self?.bar = bar }
    }
}

private struct ComposeHost: UIViewControllerRepresentable {
    let screen: IosScreen

    func makeUIViewController(context: Context) -> UIViewController { screen.viewController }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// A Compose screen in a NavigationStack, with the native glass toolbar it asked for.
/// When `covered` is true (a Compose sheet is up), back, swipe-back and toolbar items are disabled.
struct HostedScreen: View {
    @StateObject private var model: ScreenModel
    var covered: Bool

    init(covered: Bool = false, _ make: @escaping () -> IosScreen) {
        self.covered = covered
        _model = StateObject(wrappedValue: ScreenModel(make()))
    }

    var body: some View {
        ComposeHost(screen: model.screen)
            // Compose pads for the bars, the home indicator and the keyboard itself, from the
            // safe area UIKit gives its view; letting SwiftUI inset it too would double them.
            .ignoresSafeArea()
            .background(Color.spyBackground.ignoresSafeArea())
            .navigationTitle(model.bar?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(model.bar?.cancel != nil || covered)
            .toolbar {
                if let cancel = model.bar?.cancel {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button { model.screen.tapCancel() } label: { Image(systemName: cancel.symbol) }
                            .accessibilityLabel(cancel.label)
                            .disabled(covered)
                    }
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    let items = model.bar?.items ?? []
                    ForEach(Array(items.enumerated()), id: \.offset) { i, item in
                        if item.menu.isEmpty {
                            Button(role: item.danger ? .destructive : nil) {
                                model.screen.tap(item: Int32(i), menuItem: -1)
                            } label: { Label(item.label, systemImage: item.symbol) }
                                .disabled(covered)
                        } else {
                            Menu {
                                ForEach(Array(item.menu.enumerated()), id: \.offset) { j, entry in
                                    Button(role: entry.danger ? .destructive : nil) {
                                        model.screen.tap(item: Int32(i), menuItem: Int32(j))
                                    } label: { Label(entry.label, systemImage: entry.symbol) }
                                }
                            } label: { Label(item.label, systemImage: item.symbol) }
                                .disabled(covered)
                        }
                    }
                }
            }
    }
}

extension Color {
    /// `bg` from the design tokens; shows behind the bars and while a screen is still loading.
    static let spyBackground = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x12 / 255, green: 0x11 / 255, blue: 0x10 / 255, alpha: 1)
            : UIColor(red: 0xF6 / 255, green: 0xF2 / 255, blue: 0xEA / 255, alpha: 1)
    })
}

private let shell = IosShell.shared

/// Igraj: home, with setup pushed over it.
private struct PlayTab: View {
    let state: ShellState

    var body: some View {
        let path = Binding<[String]>(
            get: { state.setupOpen ? ["setup"] : [] },
            set: { if $0.isEmpty { shell.closeSetup() } }
        )
        NavigationStack(path: path) {
            HostedScreen { shell.home() }
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(state.homeSheet ? .hidden : .automatic, for: .tabBar)
                .navigationDestination(for: String.self) { _ in
                    HostedScreen { shell.setup() }
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }
}

/// Rječnici: the list, then a dictionary and its categories pushed over it.
private struct DictionariesTab: View {
    let state: ShellState

    var body: some View {
        let path = Binding<[String]>(
            get: { state.dictionaryPath },
            set: { shell.popDictionaries(path: $0) }
        )
        // The full-screen QR draws its own back; sheets are Compose, so the tab bar steps aside
        // and the native back/toolbar items are disabled while a sheet is up.
        let bars: Visibility = state.overlay == 2 ? .hidden : .automatic
        let covered = state.overlay != 0
        NavigationStack(path: path) {
            HostedScreen(covered: covered) { shell.dictionaryList() }
                .toolbar(bars, for: .navigationBar)
                .toolbar(covered ? .hidden : .automatic, for: .tabBar)
                .navigationDestination(for: String.self) { key in
                    HostedScreen(covered: covered) { shell.dictionary(key: key) }
                        .toolbar(bars, for: .navigationBar)
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }
}

/// The app-switcher snapshot must never show a role or the word (decisions.md §4, motion.md §11):
/// cover the UI whenever the scene isn't active. Solid, because a thin material still lets the
/// large display text through. The game cover is presented above the tabs, so it needs its own.
private struct PrivacyCover: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content.overlay {
            if scenePhase != .active {
                Color.spyBackground.ignoresSafeArea()
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var model = ShellModel()

    var body: some View {
        Group {
            if let state = model.state {
                let tab = Binding<Int32>(get: { state.tab }, set: { shell.selectTab(index: $0) })
                let inGame = Binding<Bool>(get: { state.inGame }, set: { _ in })
                TabView(selection: tab) {
                    PlayTab(state: state)
                        .tabItem { Label(state.tabTitles[0], image: state.tab == 0 ? "TabPlaySelected" : "TabPlay") }
                        .tag(Int32(0))
                    DictionariesTab(state: state)
                        .tabItem { Label(state.tabTitles[1], image: "TabDictionaries") }
                        .tag(Int32(1))
                }
                .tint(.primary)
                // The game is immersive and leaves only through its own ✕ (with a confirm) or Game over.
                .fullScreenCover(isPresented: inGame) {
                    HostedScreen { shell.game() }
                        .ignoresSafeArea()
                        .interactiveDismissDisabled()
                        .modifier(PrivacyCover())
                }
            } else {
                Color.spyBackground.ignoresSafeArea()
            }
        }
        .modifier(PrivacyCover())
    }
}
