COIN: a distributed accounting system for peer to peer networks
===============================================================

_A project by [**Fabio Varesano**](http://www.varesano.net/contents/projects/coin%20distributed%20accounting%20system%20peer%20peer%20networks)_

This is the project I've created and developed for my Bachelor of Science degree at the [Department of Computer Science](http://www.di.unito.it/) of the [University of Torino](http://www.unito.it/), Italy. During the project, mentored by [Professor Giancarlo Ruffo](http://www.di.unito.it/~ruffo/), I worked on a decentralized accounting system for peer to peer networks.

You can read a [little introduction to the project](http://www.varesano.net/files/FabioVaresano-COIN_English_Intro.pdf) where I analyze what are the goals of my project.

Protocol
--------

The main part of the project was the designing of an accounting protocol for a P2P network. I based my work on [Pastry](http://freepastry.org/PAST/overview.pdf) and [David Hausheer](http://hausheer.osola.com/)'s [PeerMint](http://hausheer.osola.com/publications/networking05-camera-ready.pdf). This part has been covered deeply on [my thesis](http://www.varesano.net/files/FabioVaresano-COIN.pdf) (in Italian). I suggest reading it if you need detailed protocol information.

Implementation
--------------

The second part of the project was implementing the P2P accounting protocol. This has been done using [FreePastry](http://www.freepastry.org/), an open-source Java implementation of Pastry intended for deployment in the Internet.

The implementation is only partial as our goal was just to demonstrate the behavior and the correctness of the protocol.