# PirateerLiteAndroid

[![TravisCI](https://travis-ci.org/prespondek/PirateerLiteAndroid.svg?branch=master)](https://travis-ci.org/prespondek/PirateerLiteAndroid)

#### https://github.com/prespondek/PirateerLiteAndroid/tree/master/app/src/main/java/com/lanyard

Clear For Action Lite. Non trivial sample Android game written in Kotlin.

    $ git clone https://github.com/prespondek/PirateerLiteAndroid

Build with Android Studio

Please note that assets have been stripped so you can compile the project but it will error when debugging.

Full app can be downloaded from google play store here:
[https://play.google.com/store/apps/details?id=com.lanyard.pirateerlite][50]

## Screenshots

<img src=https://i.imgur.com/Du6S9CH.png height=300 align=left>
<img src=https://i.imgur.com/9L7kkRf.png height=300 align=left>
<img src=https://i.imgur.com/PabK8xU.png height=300>

## Libraries Used

* [Foundation][0] - Components for core system capabilities, Kotlin extensions and support for
  multidex and automated testing.
  * [AppCompat][1] - Degrade gracefully on older versions of Android.
  * [Test][4] - An Android testing framework for unit and runtime UI tests.
* [Architecture][10] - A collection of libraries that help you design robust, testable, and
  maintainable apps. Start with classes for managing your UI component lifecycle and handling data
  persistence.
  * [Lifecycles][12] - Create a UI that automatically responds to lifecycle events.
  * [LiveData][13] - Build data objects that notify views when the underlying database changes.
  * [Room][16] - SQLite database with in-app objects and compile-time checks.
  * [ViewModel][17] - Store UI-related data that isn't destroyed on app rotations. Easily schedule
     asynchronous tasks for optimal execution.
  * [Threads][38] - App has a seperate render thread.
  * [Executors][43] - To save memory images are constantly un/loaded based on viewport position.
  * [Kotlin Coroutines][91] for managing background threads with simplified code and reducing needs for callbacks
* [UI][30] - Details on why and how to use UI Components in your apps - together or separate
  * [Auto Rotate][34] - App supports different layouts. Single view on handheld, and multiview on tablet. 
  * [Animations & Transitions][31] - Move widgets and transition between screens.
  * [Fragment][34] - A basic unit of composable UI.
  * [Layout][35] - Lay out widgets using different algorithms.
  * [ScrollView, Scrollview Adapter][39] - Boatlist, Market, Shipyard menus
  * [Gesture Detector][40] - Swipe gesture for cargo selection.
  * [Grid Layout Manager][41] - Jobs grid layout
  * [Surface View][42] - Renders the Map view 
  * [Backstack][45] - Handles back button behaviour. A horrible, immalleable thing, that will probably not do what you want.
* Third party
  * [GSon][36] Easier JSON parsing
  * [Super Scroll View][37] Scrollview that works in vertical and horizontal directions. Handles map scrolling and interaction.
  
## CanvasKit:
#### https://github.com/prespondek/PirateerLiteAndroid/tree/master/app/src/main/java/com/lanyard/canvas
I utilised Apple's SpriteKit for the IOS version, but no such thing exists on Android. I couldn't find a suitable kotlin alternative so ended up writing my own using surface view. It behaves similar to SpriteKit or Cocos2D, but uses the android canvas rather than opengl. I have ported some of the basic actions and nodes to CanvasKit. SurfaceView does not use hardware acceleration so the performance is not great on some devices. I'm currently looking at converting it to a GLSurfaceView.
  
## Comments:

In hindsight I should have built this Android version first. Porting the IOS code to Android was a good deal more difficult than I expected mainly due to the activity lifecycle. Kotlin is also deceptively similar to swift but is actually different enough to make porting swift code by hand annoying. Also, wrangling the fragment backstack is a nightmare if you have an irregular UI flow. In future I would recommend not using it at all and just overriding onBackPressed.   

## License

Copyright 2019 Peter Respondek.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[0]: https://developer.android.com/jetpack/components
[1]: https://developer.android.com/topic/libraries/support-library/packages#v7-appcompat
[2]: https://developer.android.com/kotlin/ktx
[4]: https://developer.android.com/training/testing/
[10]: https://developer.android.com/jetpack/arch/
[11]: https://developer.android.com/topic/libraries/data-binding/
[12]: https://developer.android.com/topic/libraries/architecture/lifecycle
[13]: https://developer.android.com/topic/libraries/architecture/livedata
[14]: https://developer.android.com/topic/libraries/architecture/navigation/
[16]: https://developer.android.com/topic/libraries/architecture/room
[17]: https://developer.android.com/topic/libraries/architecture/viewmodel
[18]: https://developer.android.com/topic/libraries/architecture/workmanager
[30]: https://developer.android.com/guide/topics/ui
[31]: https://developer.android.com/training/animation/
[34]: https://developer.android.com/guide/components/fragments
[35]: https://developer.android.com/guide/topics/ui/declaring-layout
[36]: https://sites.google.com/site/gson/Home
[38]: https://developer.android.com/guide/components/processes-and-threads
[37]: https://github.com/huangmb/SuperScrollView
[90]: https://bumptech.github.io/glide/
[91]: https://kotlinlang.org/docs/reference/coroutines-overview.html
[42]: https://www.google.com/url?client=internal-uds-cse&cx=000521750095050289010:zpcpi1ea4s8&q=https://developer.android.com/reference/android/view/SurfaceView&sa=U&ved=2ahUKEwiR1Ie0iarlAhUBN48KHfJwDtIQFjAAegQIBRAB&usg=AOvVaw1xtZEaZcXdkxTKRyLCJf2z
[39]: https://developer.android.com/reference/android/widget/ScrollView
[43]: https://developer.android.com/reference/java/util/concurrent/Executor
[45]: https://developer.android.com/guide/components/activities/tasks-and-back-stack
[40]: https://developer.android.com/training/gestures/detector
[41]: https://developer.android.com/reference/android/widget/GridLayout
[50]: https://play.google.com/store/apps/details?id=com.lanyard.pirateerlite
