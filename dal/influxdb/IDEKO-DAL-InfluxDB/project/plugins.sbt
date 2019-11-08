addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.15")


resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)



// Dockerizing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")