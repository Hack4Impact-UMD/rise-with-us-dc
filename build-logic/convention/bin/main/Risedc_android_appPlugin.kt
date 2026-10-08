/**
 * Precompiled [risedc.android.app.gradle.kts][Risedc_android_app_gradle] script plugin.
 *
 * @see Risedc_android_app_gradle
 */
public
class Risedc_android_appPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Risedc_android_app_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
