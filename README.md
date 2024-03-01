# VolVis
![VolVis](https://github.com/sanderjurgens/volvis/blob/main/img/volvis.png)

## What is it?
VolVis is an application that allows the testing of various volume rendering techniques.
From the user interface one can load volumes defined in the [AVS Field File Format](https://vis.lbl.gov/archive/NERSC/Software/express/help6.1/help/reference/dvmac/Field_Fi.htm), the default being a piggy bank, and apply a multitude of rendering algorithms to display its density data in different ways.
These algorithms range from multithreaded ray casting based on [Maximum Intensity Projection](https://wikipedia.org/wiki/Maximum_intensity_projection) or [Isovalue Contour Surfaces](https://graphics.stanford.edu/papers/volume-cga88/volume.pdf)
to 3 dimensional rendering techniques such as [Marching Cubes](https://wikipedia.org/wiki/Marching_cubes), all to provide insight into the density values of the given volume.
Additional color options are also provided using a transfer function that assigns RGB and opacity values to a density.

## Why did I make it?
VolVis is an old (anno 2012) project, from my time studying at [TU/e](https://www.tue.nl/en/). 
Eindhoven being the birthplace of Philips, partially known for the medical systems of its Healthcare division, we were challenged by them to think about and implement meaningful ways to visualize data coming from their MRI machines.
By that time I had already developed an interest in complex algorithms, but during this project I learned that visualizing and interacting with the result made solving difficult problems a lot more exciting.
This is why most of my projects since then include an element of visualization and user interaction.

## How does it work?
VolVis provides intuitive ways of visualizing of a given volume, which are all stored in the [data](https://github.com/SanderJurgens/volvis/tree/main/data) folder. 
Most controls speak for themselves, but some extra explanation might be required for the transfer function tab:
- Add point = Click left mouse button
- Move point = Hold left mouse button and drag
- Recolor point = Click middle mouse button
- Remove point = Click right mouse button

If you want to learn more about the codebase, see the [Javadoc](https://sanderjurgens.github.io/volvis/). 
Specifically for developers, please note the [bug](https://github.com/jzy3d/jogl/issues/4) present in JOGL 2.5 when run on Java 17 and Windows 10.
To run the codebase from your IDE add the mentioned arguments to your configuration, or simply execute the "mvn exec:exec" command since it already includes these arguments.

If you just want to see the application in action, download this [jar](https://github.com/sanderjurgens/volvis/blob/main/target/volvis-1.1-fat.jar). 