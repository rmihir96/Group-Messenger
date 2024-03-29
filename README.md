# Group-Messenger-with-Total-and-FIFO-Ordering-Guarantees

## Introduction

We are now ready to implement more advanced concepts and in this assignment you will add ordering guarantees to your group messenger. The guarantees you will implement are total ordering as well as FIFO ordering. As with part A, you will store all the messages in your content provider. The different is that when you store the messages and assign sequence numbers, your mechanism needs to provide total and FIFO ordering guarantees.  Once again, please follow everything exactly. Otherwise, it might result in getting no point for this assignment.

Step 0: Importing the project template
Once again, you need to download a template to start this assignment.
Download the project template zip file to a temporary directory.
Extract the template zip file and copy it to your Android Studio projects directory.
Make sure that you copy the correct directory. After unzipping, the directory name should be “GroupMessenger2”, and right underneath, it should contain a number of directories and files such as “app”, “build”, “gradle”, “build.gradle”, “gradlew”, etc.
After copying, delete the downloaded template zip file and unzipped directories and files. This is to make sure that you do not submit the template you just downloaded. (There were many people who did this before.)
Open the project template you just copied in Android Studio.
Use the project template for implementing all the components for this assignment.

Step 1: Writing a Content Provider
As with the previous assignment, you need to have a working content provider. The requirements are almost exactly the same as the previous assignment. The only exception is the URI, which is “content://edu.buffalo.cse.cse486586.groupmessenger2.provider”.

Step 2: Implementing Total and FIFO Ordering Guarantees
This is the meat of this assignment and you need to implement total and FIFO guarantees. You will need to design an algorithm that does this and implement it. An important thing to keep in mind is that there will be a failure of an app instance in the middle of the execution. The requirements are:
Your app should multicast every user-entered message to all app instances (including the one that is sending the message). In the rest of the description, “multicast” always means sending a message to all app instances.
Your app should use B-multicast. It should not implement R-multicast.
You need to come up with an algorithm that provides a total-FIFO ordering under a failure.
There will be at most one failure of an app instance in the middle of execution.  We will emulate a failure only by force closing an app instance. We will not emulate a failure by killing an entire emulator instance. When a failure happens, the app instance will never come back during a run.
Each message should be used to detect a node failure.
For this purpose, you can use a timeout for a socket read; you can pick a reasonable timeout value (e.g., 500 ms), and if a node does not respond within the timeout, you can consider it a failure.
This means that you need to handle socket timeout exceptions in addition to socket creation/connection exceptions.
Do not just rely on socket creation or connect status to determine if a node has failed. Due to the Android emulator networking setup, it is not safe to just rely on socket creation or connect status to judge node failures. Please also use socket read timeout exceptions as described above.
You cannot assume which app instance will fail. In fact, the grader will run your group messenger multiple times and each time it will kill a different instance. Thus, you should not rely on chance (e.g., randomly picking a central sequencer) to handle failures. This is just hoping to avoid failures. Instead, you should implement a decentralized algorithm (e.g., something based on ISIS).
When handling a failure, it is important to make sure that your implementation does not stall. After you detect a failure, you need to clean up any state related to it, and move on.
When there is a node failure, the grader will not check how you are ordering the messages sent by the failed node. Please refer to the testing section below for details.
As with the previous PAs, we have fixed the ports & sockets.
Your app should open one server socket that listens on 10000.

You need to use run_avd.py and set_redir.py to set up the testing environment.
The grading will use 5 AVDs. The redirection ports are 11108, 11112, 11116, 11120, and 11124.
You should just hard-code the above 5 ports and use them to set up connections.

Please use the code snippet provided in PA1 on how to determine your local AVD.
emulator-5554: “5554”

emulator-5556: “5556”

emulator-5558: “5558”

emulator-5560: “5560”

emulator-5562: “5562”

Every message should be stored in your provider individually by all app instances. Each message should be stored as a <key, value> pair. The key should be the final delivery sequence number for the message (as a string); the value should be the actual message (again, as a string). The delivery sequence number should start from 0 and increase by 1 for each message.
For your debugging purposes, you can display all the messages on the screen. However, there is no grading component for this.
Please read the notes at the end of this document. You might run into certain problems, and the notes might give you some ideas about a couple of potential problems.

## Testing

We have testing programs to help you see how your code does with our grading criteria. If you find any rough edge with the testing programs, please report it on Piazza so the teaching staff can fix it. The instructions are the following:
Download a testing program for your platform. If your platform does not run it, please report it on Piazza.

Windows: We’ve tested it on 32- and 64-bit Windows 8.

Linux: We’ve tested it on 32- and 64-bit Ubuntu 12.04.

OS X: We’ve tested it on 32- and 64-bit OS X 10.9 Mavericks.

Before you run the program, please make sure that you are running five AVDs. python run_avd.py 5 will do it.
Please also make sure that you have installed your GroupMessenger2 on all the AVDs.
Run the testing program from the command line.

There are two phases of testing

Phase 1---Testing without any failure: In this phase, all the messages should be delivered in a total-FIFO order. For each message, all the delivery sequence numbers should be the same across processes.

Phase 2---Testing with a failure: In this phase, all the messages sent by live nodes should be delivered in a total-FIFO order. Due to a failure, the delivery sequence numbers might go out of sync if some nodes deliver messages from the failed node, while others do not. This is OK; the grader will only examine the total-FIFO ordering guarantees for the messages sent by live nodes. (Note: in phase 2, the message sequence numbers can go out of sync due to a failure. Thus, when the grader output says that a key is missing, the key means the message sequence number that the grader is verifying. It may not be the exact key.)

Once again, you should implement a decentralized algorithm to handle failures correctly. This means that you should not implement a centralized algorithm. This also means that you should not implement any variation of a centralized algorithm that randomly picks a central node. In our grading, we will run phase 2 as many as possible.
If your implementation uses randomness in failure handling or is centralized, the score you get through the grader is not guaranteed.
On your terminal, it will give you your partial and final score, and in some cases, problems that the testing program finds.
Unlike previous graders, the grader for this assignment requires you to directly give the path of your apk to it.

The grader will take care of installing/uninstalling the apk as necessary.
The grader uses multiple threads to test your code and each thread will independently print out its own log messages. This means that an error message might appear in the middle of the combined log messages from all threads, rather than at the end.
The grader has many options you can use for your testing. It allows you to choose which phase to test and for phase 2, how many times to run. It also has an option to print out verbose output, which can be helpful for debugging. You can enter the following command to see the options:

$ <grader executable> -h


You might run into a debugging problem if you're reinstalling your app from Android Studio. This is because your content provider will still retain previous values even after reinstalling. This won't be a problem if you uninstall explicitly before reinstalling; uninstalling will delete your content provider storage as well. In order to do this, you can uninstall with this command:

$ adb uninstall edu.buffalo.cse.cse486586.groupmessenger2

## Credits:
Prof. Steve Ko - https://nsr.cse.buffalo.edu/?page_id=272

Course : CSE 586 - Distributed Systems - https://cse.buffalo.edu/~stevko/courses/cse486/spring19/
