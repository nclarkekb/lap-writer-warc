1. Overview:
------------

LAP WARC writer is netarkivet.dk's contribution to INA's Live Archiching Proxy (LAP) IIPC funded project.
The enclosed project adds a WARC writer to LAP.
This writer should be compatible with LAP v1.2.1.


2. Introduction:
----------------

LAP is a web proxy written in Perl. LAP is started from a console.
To use it you just edit the proxy setting in your browser (or derived web client).
Everything that passes through LAP can potentially be written to disk.
LAP sends all requests/responses to which ever writer is attached.
If no writer is attached, the data is cached and sent to the first writer that attaches itself.
It is also possible to configure LAP to discard data if no writer is currently attached.
Each writer decides what to write and which storage format to use.


3. Requirements:
----------------

64bit Linux to run the LAP application.

One or more client machine(s) with Java1.6+ installed to run the WARC writer.


4. Running from console:
------------------------

The writer can be run from any machine with a recent Java JRE or JDK installed (6+)

Running the WARC writer without arguments will show a usage message.

	java -jar lap-writer-warc-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar

Usage:
lap-writer-arc lap-host:lap-port
  --dir=warcFileTargetDirectory
  [--ip=ip]
  [--prefix=fileNamesPrefix]
  [--compression=true|false]
  [--compress=true|false]
  [--max-file-size=maxArcFileSize]
  [--timeout=connectionTimeout]
  [--deduplication=true|false]
  [--ispartof=warcinfo ispartof]
  [--description=warcinfo description]
  [--operator=warcinfo operator]
  [--httpheader=warcinfo httpheader]
  [--verbose]
  [--config=configfil.json]

C:\Java\workspace\lap-writer-warc>

Options enclosed with [] are currently optional. Although ispartof and operator should probably always be supplied.

prefix defaults to "LAP".
compression/compress defaults to false.
max-file-size defaults to 1GB.
timeout defaults to 10 seconds.
deduplication default to true.

ip can be used to filter clients whos browsed data will not be written.


5. Configuration:
-----------------

To start a WARC writer you just need to supply the correct number of parameters.
The normal parameters can only be used to configure the WARC writer for a single user.

If you want to configure the WARC writer to write to different WARC files based on IP you need to use the --config option.
The configuration file is in the JSON format.

An example of such a files is reproduced here.
Note that there are two client configured in this example.
You can also choose to use this mode with one client, if you want to avoid using long command lines.

========
{
        "timeout": 10,
        "verbose": true,
        "sessions": [
                {
                        "dir": ".",
                        "ip": ["10.6.0.211"],
                        "prefix": "LAP-KB-SB-TEST",
                        "compression": false,
                        "max-file-size": 12345678,
                        "deduplication": false,
                        "ispartof": "LAP Test",
                        "description": "Files archive as part of testing INA's LAP.",
                        "operator": "KB/SB",
                        "httpheader": "LAP Test samling"
                },
                {
                        "dir": ".",
                        "ip": ["10.6.0.191"],
                        "prefix": "LAP-KB-SB-TEST",
                        "compression": false,
                        "max-file-size": 12345678,
                        "deduplication": false,
                        "ispartof": "LAP Test",
                        "description": "Files archive as part of testing INA's LAP.",
                        "operator": "KB/SB",
                        "httpheader": "LAP Test samling"
                }
        ]
}
========

timeout and verbose are global writer parameters.
Inside the sessions object one or more sessions can be configured.
As with the normal parameters, dir is required.
prefix, compress/compression, max-file-size and deduplication defaults to the same values as with normal parameters.

If required a more formal definition of the JSON format can be found here: http://www.json.org/


5a. Single user example:
------------------------

All argument except "--config" are used to run the writer in single user mode.

You could run the writer with the following command:

	java -jar lap-writer-warc-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar dia-prod-udv-01:4365 --dir=. --verbose --ispartof="Internet Archive world harvest 2000-2004" --description="Retrieved from Internet Archive November 2009 as arc-files, converted to warc-files december 2010." --operator="Internet Archive" --httpheader="Retrospektiv indsamling 2000-2004"

The warcinfo would then include this:

software: LAP WARC writer v0.5
host: kb007268
isPartOf: Internet Archive world harvest 2000-2004
description: Retrieved from Internet Archive November 2009 as arc-files, converted to warc-files december 2010.
operator: Internet Archive
httpheader: Retrospektiv indsamling 2000-2004
format: WARC file version 1.0
conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf


5b. Multi user example:
-----------------------

Using the JSON configuration from above the writer can be run using the following command

	java -jar lap-writer-warc-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar dia-prod-udv-01:4365 --config=lap.json


6. Deduplication:
-----------------

If deduplication is disabled everything that is passed to the writer is stored in the WARC file.

If duduplication is enabled content to be stored falls into one of three categories:

  New
    The content is stored and the deduplication structure is updated with WARC-Record-ID, Content-Length and WARC-Target-URI.

  Identical
    The content has already been stored with the same WARC-Payload-Digest, Content-Length and WARC-Target-URI.
    Nothing is stored.

  Duplicate
    The content has already been stored with the same WARC-Payload-Digest and Content-Length but under a different WARC-Target-URI.
    A WARC revisit record is written to the WARC file with a WARC-Refers-To pointing to the WARC record with the content.
    The revisit record also includes the original WARC-Payload-Digest and the new WARC-Target-URI.
    The deduplication structure is updated with the new WARC-Target-URI.
    The WARC-Profile used is the one suggested by the Heritrix3 developers, "http://netpreserve.org/warc/1.0/revisit/uri-agnostic-identical-payload-digest".


7. Technicals:
--------------

Deduplication is currently handled by using a berkeleydb persisted map.
These files are stored in the tmp folder on your operating system and should be deleted when the writer exits.
The tmp folder is also used to storing large content prior to writing it to a WARC file for the purpose of digesting it.


8. Resources:
-------------

The LAP project is available from the following location.

https://github.com/INA-DLWeb/LiveArchivingProxy

The LAP WARC writer source code is available from the following mercurial repository.

https://bitbucket.org/nclarkekb/lap-writer-warc


9. Future:
----------

(*) Make all dependencies available in public repositories.

(*) Using the command line is not the most user friendly approach.
    Work has been done to implement a webapp which could be used to configure WARC Writer sessions.
    This feature however was not included in the original project proposal and therefor currently not scheduled.


10. Contact:
------------

nicl[at]kb[dot]dk
