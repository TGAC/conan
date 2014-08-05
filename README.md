Conan - lightweight workflow manager
====================================

Conan is an extremely light-weight workflow management application. It is designed to provide a user-friendly 
interface to interact with various components of the "script-chaining" types workflows that are typical in 
bioinformatics.


Description
-----------

Conan was developed to handle the various loading scenarios and workflows involved in the ArrayExpress and the 
GXA databases. However, it is available as a standalone tool and can be customised to chain processes (for 
example, Java processes, perl or shell scripts) together in a resuable way. It is possible to install the Conan 
web application in your own environments and put together a new workflow with a minimum amount of development.

Conan was extended to handle "execution contexts": it will adapt how it calls out to external processes, such as 
external binaries or scripts, based on the spcecified environment.  For example, conan can be instructed to 
execute commands in a scheduled environment such as LSF or PBS, or on a local unscheduled machine.  Users can
also incorperate their own commands that should be executed prior to processes in the conan pipeline.  This is
useful if you must run commands to load software in your environment.

If you are interested in installing Conan and developing your own processes, see the developer documentation 
on the Github wiki.


Issues and support
------------------

To request a new feature or if you think you've found a bug, please raise an issue in the Github Tracker


Support
-------
If you need help using this software, please read the documentation in the Github wiki, or email Daniel Mapleson
(daniel.mapleson@tgac.ac.uk) or Tony Burdett (tburdett@ebi.ac.uk).


Contact
-------

For more information or to get involved please email Daniel Mapleson (daniel.mapleson@tgac.ac.uk) or Tony 
Burdett (tburdett@ebi.ac.uk).


Acknowledgements
----------------

This tool is developed by Tony Burdett, Natalja Kurbatova, Emma Hastings, Adam Faulconbridge, Dan Mapleson, Rob Davey

Development of the original Conan workflow tool was paid for by EMBL-EBI core funding.  Conan was extended by TGAC.
