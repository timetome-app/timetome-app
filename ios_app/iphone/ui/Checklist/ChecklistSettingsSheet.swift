import SwiftUI
import shared

struct ChecklistSettingsSheet: View {
    
    let checklistDb: ChecklistDb?
    let onSave: (ChecklistDb) -> Void
    let onDelete: () -> Void

    var body: some View {
        
        VmView({
            ChecklistSettingsVm(checklistDb: checklistDb)
        }) { vm, state in
            ChecklistSettingsScreenInner(
                vm: vm,
                state: state,
                name: state.name,
                onSave: onSave,
                onDelete: onDelete
            )
        }
    }
}

private struct ChecklistSettingsScreenInner: View {
    
    let vm: ChecklistSettingsVm
    let state: ChecklistSettingsVm.State
    @State var name: String
    let onSave: (ChecklistDb) -> Void
    let onDelete: () -> Void
    
    ///
    
    @FocusState private var isFocused: Bool
    @Environment(\.dismiss) private var dismiss
    @Environment(Navigation.self) private var navigation
    
    var body: some View {
        
        List {
            
            TextField(
                text: $name,
                prompt: Text(state.namePlaceholder)
            ) {
            }
            .focused($isFocused)
            .onChange(of: name) { _, new in
                vm.setName(name: new)
            }
            
            if let checklistDb = state.checklistDb {
                Section {
                    Button(state.deleteButtonText) {
                        vm.delete(
                            checklistDb: checklistDb,
                            dialogsManager: navigation,
                            onDelete: {
                                dismiss()
                                onDelete()
                            }
                        )
                    }
                    .foregroundColor(.red)
                }
            }
        }
        .contentMargins(.top, 14)
        .interactiveDismissDisabled()
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(state.title)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(state.saveButtonText) {
                    vm.save(
                        dialogsManager: navigation,
                        onSuccess: { newChecklistDb in
                            dismiss()
                            onSave(newChecklistDb)
                        }
                    )
                }
                .fontWeight(.bold)
                .disabled(!state.isSaveEnabled)
            }
        }
        .onAppear {
            isFocused = true
        }
    }
}
