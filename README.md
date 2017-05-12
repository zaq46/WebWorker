# WebWorker
The WebLoader program loads a list url strings from a file. It presents the urls in the left column of a table. When one of the Fetch buttons is clicked, it forks off one or more threads to download the HTML for each url. A Stop button can kill off the downloading threads if desired. A progress bar and some other status fields show the progress of the downloads -- the number of worker threads that have completed their run, the number of threads currently running, and (when done running) the elapsed time. 