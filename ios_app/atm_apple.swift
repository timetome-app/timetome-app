import SwiftUI
import Combine
import shared

struct VMView<VMState: AnyObject, Content: View>: View {

    private let vm: __Vm<VMState>
    @State private var state: VMState
    private let publisher: AnyPublisher<VMState, Never>
    @ViewBuilder private let content: (VMState) -> Content
    private let stack: StackType

    init(
        vm: __Vm<VMState>,
        stack: StackType = .ZStack(),
        @ViewBuilder content: @escaping (VMState) -> Content
    ) {
        self.vm = vm
        state = vm.state.value as! VMState
        publisher = vm.state.toPublisher()
        self.stack = stack
        self.content = content
    }

    var body: some View {
        ZStack {
            switch stack {
            case .ZStack(let p1):
                ZStack(alignment: p1) {
                    content(state)
                }
            case .VStack(let p1, let p2):
                VStack(alignment: p1, spacing: p2) {
                    content(state)
                }
            case .HStack(let p1, let p2):
                HStack(alignment: p1, spacing: p2) {
                    content(state)
                }
            }
        }
            // In onAppear() because init() is called frequently even the
            // view is not showed. "Unnecessary" calls is 90% times.
            // Yes, onAppear() calls too late but the default values DI saves.
                .onAppear {
                    vm.onAppear()
                }
                .onDisappear {
                    vm.onDisappear()
                }
                ////
                .onReceive(publisher) { res in
                    state = res
                }
    }

    enum StackType {
        case ZStack(alignment: Alignment = .center)
        case VStack(alignment: HorizontalAlignment = .center, spacing: CGFloat? = 0)
        case HStack(alignment: VerticalAlignment = .center, spacing: CGFloat? = 0)
    }
}
