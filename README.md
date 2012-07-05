imagej-fastlevelset
===================

This is an implementation of the fast level set published in [1] for ImageJ, along with some additional stuff for tracking and measuring objects.
Use Ant (from Apache) to build using build.xml, then copy the jar file from build/jar/ into your ImageJ plugins directory.

This was originally written in C++ as a Matlab Mex library during my PhD.
Recently I decided to reimplement it as an ImageJ plugin to practice my Java skills.
The level-set is more or less complete apart from the edge speed field, the rest is still a work in progress.


[1] Real-time Tracking Using Level Sets. 2005. Yonggang Shi, W Clem Karl. IEEE CVPR.



