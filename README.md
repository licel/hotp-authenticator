# HOTP Authenticator App

The **HOTP Authenticator App** is a demonstration project showcasing the integration of a Trusted Application (TA) developed using **Java Card Technology** into an Android mobile application. 
The Trusted Application is prototyped with the help of **jCardSim**, an open-source simulator for Java Card applications. This project supports the **[Licel Droidcon Hackathon](https://github.com/licel/droidcon-hackathon)**, 
providing developers with a starting point to explore the development of secure and trusted mobile applications.

## Features
- **HOTP Generation**: Implements the HMAC-based One-Time Password (HOTP) algorithm as defined in [RFC 4226](https://tools.ietf.org/html/rfc4226).
- **Java Card Simulation**: The Trusted Application is developed and tested using **jCardSim**.
- **Mobile Integration**: The project demonstrates embedding the TA into an Android application for secure authentication workflows.

## Key Components
- **jCardSim**: Simulates the Java Card runtime environment for easy development and testing of Java Card applets.
- **Trusted Application**: A secure Java Card applet implementing HOTP logic.
- **Android Application**: Provides a user-friendly interface for scanning QR codes, managing secrets, and generating HOTP values.

## Getting Started
### Prerequisites
- **Android Studio**: Required for building and running the mobile application.
- **jCardSim Library**: Included in the project dependencies.

