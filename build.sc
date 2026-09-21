import mill._
import mill.scalalib._

object app extends JavaModule {
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      coursier.MavenRepository("https://maven.google.com"),
      coursier.MavenRepository("https://jitpack.io")
    )
  }

  def sources = T.sources {
    super.sources() ++ Seq(PathRef(os.pwd / "build_apk" / "gen"))
  }

  def javacOptions = Seq("-source", "1.8", "-target", "1.8")

  def unmanagedClasspath = T {
    Agg(
      PathRef(os.Path("/opt/android-sdk/platforms/android-34/android.jar")),
      PathRef(os.pwd / "libs" / "onnxruntime.jar")
    )
  }

  def ivyDeps = Agg(
    ivy"com.github.TeamNewPipe:NewPipeExtractor:v0.22.7",
    ivy"com.squareup.okhttp3:okhttp:4.12.0",
    ivy"org.mozilla:rhino:1.7.14"
  )
}
