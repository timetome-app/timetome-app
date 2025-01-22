import SwiftUI
import shared

struct TaskFoldersFormSheet: View {
    
    var body: some View {
        VmView({
            TaskFoldersFormVm()
        }) { vm, state in
            TaskFoldersFormSheetInner(
                vm: vm,
                state: state,
                foldersDbAnimate: state.foldersDb,
                tmrwButtonUiAnimation: state.tmrwButtonUi
            )
        }
    }
}

private struct TaskFoldersFormSheetInner: View {
    
    let vm: TaskFoldersFormVm
    let state: TaskFoldersFormVm.State
    
    @State var foldersDbAnimate: [TaskFolderDb]
    @State var tmrwButtonUiAnimation: TaskFoldersFormVm.TmrwButtonUi?
    
    ///
    
    @Environment(\.dismiss) private var dismiss
    @Environment(Navigation.self) private var navigation
    
    @State private var editMode: EditMode = .active
    
    @State private var withFoldersAnimation = true
    
    ///
    
    var body: some View {
        
        List {
            
            ForEach(foldersDbAnimate, id: \.id) { folderDb in
                Button(folderDb.name) {
                    navigation.sheet {
                        TaskFolderFormSheet(
                            taskFolderDb: folderDb
                        )
                    }
                }
                .foregroundColor(.primary)
            }
            .onMoveVm { from, to in
                withFoldersAnimation = false
                vm.moveIos(from: from, to: to)
            }
            
            Section {
                
                Button("New Folder") {
                    navigation.sheet {
                        TaskFolderFormSheet(
                            taskFolderDb: nil
                        )
                    }
                }
            }
            
            if let tmrwButtonUi = tmrwButtonUiAnimation {
                Section {
                    Button(tmrwButtonUi.text) {
                        tmrwButtonUi.add(
                            dialogsManager: navigation
                        )
                    }
                }
            }
        }
        .animateVmValue(
            value: state.foldersDb,
            state: $foldersDbAnimate,
            enabled: withFoldersAnimation,
            onChange: {
                withFoldersAnimation = true
            }
        )
        .animateVmValue(
            value: state.tmrwButtonUi,
            state: $tmrwButtonUiAnimation
        )
        .environment(\.editMode, $editMode)
        .contentMargins(.top, 14)
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(state.title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Done") {
                    dismiss()
                }
                .fontWeight(.bold)
            }
        }
    }
}
