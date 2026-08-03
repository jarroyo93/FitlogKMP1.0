import SwiftUI
import FirebaseCore
import FirebaseAuth
import Shared

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()

        // 🟢 Configuración del puente compatible con Kotlin Native
        IOSSecondaryAuthBridge.shared.handler = { correo, contrasena, completion in
            guard let defaultApp = FirebaseApp.app() else {
                completion(nil, "FirebaseApp principal no está configurado")
                return
            }

            // 🔥 Convertimos a Int64 para eliminar los decimales (.) del nombre
            let tempAppName = "TempAuthApp_\(Int64(Date().timeIntervalSince1970 * 1000))"
            FirebaseApp.configure(name: tempAppName, options: defaultApp.options)

            guard let secondaryApp = FirebaseApp.app(name: tempAppName) else {
                completion(nil, "Fallo al instanciar la app secundaria de Firebase")
                return
            }

            let secondaryAuth = Auth.auth(app: secondaryApp)
            secondaryAuth.createUser(withEmail: correo, password: contrasena) { result, error in
                // Libera la memoria de la app secundaria de inmediato
                secondaryApp.delete { _ in }

                if let error = error {
                    completion(nil, error.localizedDescription)
                } else if let uid = result?.user.uid {
                    completion(uid, nil)
                } else {
                    completion(nil, "No se obtuvo el UID del atleta creado en iOS")
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}