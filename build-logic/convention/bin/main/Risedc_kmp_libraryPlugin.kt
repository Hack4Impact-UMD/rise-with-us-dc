/**
 * Precompiled [risedc.kmp.library.gradle.kts][Risedc_kmp_library_gradle] script plugin.
 *
 * @see Risedc_kmp_library_gradle
 */
public
class Risedc_kmp_libraryPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Risedc_kmp_library_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
