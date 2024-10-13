Code by Jordan Frimpter

4 scripts included:

build.sh
launcher.sh
cleanup.sh
cleanFiles.sh

Instructions follow for using the scripts.

ALL scripts: ----------------------------------
- make sure the script has execution privileges; running the following command will grant them:

  chmod +x build.sh launcher.sh cleanup.sh cleanFiles.sh


- if at any point in running the scripts you recieve an error that is something like "token error: token is ""
  and you modify your scripts in Windows then you are experiencing conflict from the use of carriage returns.
  Run the following command on the file and try again to remove the carriage returns (<filename> is replaced with the file's name):

  sed -i -e 's/\r$//' <filename>


- These scripts are somewhat assumed to be executed in the same folder as your project; the expected execution order is:
  build.sh
  launcher.sh
  cleanup.sh
  cleanFiles.sh [optional]

- These scripts were written for a java project; you will need to modify them more heavily for compiling in c or c++.

build.sh ----------------------------------
1. Change the javac compilation to match your project structure.
2. This is just a script to automate compilation; write any commands you would use to compile here. This is not limited to java,
   you can use the gnu compiler in the same way.

launcher.sh ----------------------------------
1. Follow steps 1-4 in "Passwordless Login Instructions".
2. Change your net ID in cleanup.sh
3. Change the root directory of your project in cleanup.sh
4. Change the config file path of your project in cleanup.sh ; The config file is the same format as specified in the project instructions.
5. Change the main project class name to match your project.
6. This script will ignore carriage returns in the config file and adjust "dcxx" servers to "dcxx.utdallas.edu" automatically.
7. If your program produces useful viewable output, enable graphic display with your tunneling software [ex in mobaxterm, enable x server] to view terminal popup windows.
8. The default version of this script calls a java class and gives it a configuration file path as an argument.
   If your project calls a hard coded file path to read the configuration file, consider changing it.
   Your project may fail here if your code calls a hard coded path, as Unix will get in your way.
9. Change the run command to suit your language, if you are not using java and/or want different command arguments.
10. Run this script [from a dcxx machine] to launch terminals to other machines.
11. There is a line "sleep 1" that causes a 1 second delay between launchings. You can remove this.
   I put it there to prevent race conditions of multiple programs accessing the configuration file at the same time while debugging step 8.

cleanup.sh ----------------------------------
1. Follow steps 1-4 in "Passwordless Login Instructions".
2. Change your net ID in cleanup.sh
3. Change the root directory of your project in cleanup.sh
4. Change the config file path of your project in cleanup.sh ; The config file is the same format as specified in the project instructions.
5. This script will ignore carriage returns in the config file and adjust "dcxx" servers to "dcxx.utdallas.edu" automatically.
6. Run script; this script is designed to issue kill commands to erroneous processes.

cleanFiles.sh ----------------------------------
1. Change the rm * commands to match files you want to delete (this uses REGEX)
2. WARNING: this will delete all files matching the extensions in the folder of execution indiscriminantly.
   If you have important files with the same extension, you will want to more heavily alter the rm commands.
3. Execute to remove files matching pattern.