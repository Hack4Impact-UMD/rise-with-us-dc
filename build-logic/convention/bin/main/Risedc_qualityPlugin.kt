/**
 * Precompiled [risedc.quality.gradle.kts][Risedc_quality_gradle] script plugin.
 *
 * @see Risedc_quality_gradle
 */
public
class Risedc_qualityPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Risedc_quality_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
