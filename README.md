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

## OTPApplet APDU commands
### 1. Key management functions

#### 1.1. Import HMAC key (IMPORT_HMAC_KEY)

##### Command

| Field | Value | Definition            |
|-------|-------|-----------------------|
| CLA   | 00    |                       |
| INS   | 02    | Import HMAC key       |
| P1    | 00    |                       |
| P2    | 00    |                       |
| Lc    | VAR   | HMAC key bytes length |
| Data  |       | HMAC key bytes        |
| Le    | 01    |                       |

##### Description

Create a persistent instance of an HMAC key and return a unique ID.
Implementation notes: HMAC keys must be stored in the HMACKey[] field,
where the array index represents the unique key ID. The next key ID
needs to be calculated as the index of the first empty (null) array
element. If there are no null elements, a 6A84 error must be thrown.

##### Response


| Field  | Value | Definition    |
|--------|-------|---------------|
| Data   |       | HMAC key ID   |

##### Response codes

| SW1 | SW2 | Description                                         |
|-----|-----|-----------------------------------------------------|
| 90  | 00  | OK                                                  |
| 6D  | 00  | Instruction no supported or invalid                 |
| 6A  | 86  | Incorrect P1/P2 parameter                           |
| 6A  | 84  | There is insufficient memory space to store new key |
| 67  | 00  | Wrong length                                        |

#### 1.2. Delete HMAC key (DELETE_HMAC_KEY)

##### Command
| Field | Value | Description     |
|-------|-------|-----------------|
| CLA   | 00    |                 |
| INS   | 03    | Delete HMAC key |
| P1    | 00    |                 |
| P2    | VAR   | HMAC Key ID     |
| Lc    | 00    |                 |
| Le    | 00    |                 |

##### Description
Delete the persistent key and its metadata with the specified HMAC Key
ID. Implementation notes: Before deletion, the HMAC Key value is erased
by calling the clearKey method. The related array element must be set to
null. If the index specified by HMAC Key ID is out of range or points to
a null array element, a 6A83 error must be thrown.

##### Response codes
| SW1 | SW2 | Description                             |
|-----|-----|-----------------------------------------|
| 90  | 00  | OK                                      |
| 6D  | 00  | Instruction no supported or invalid     |
| 6A  | 86  | Incorrect P1/P2 parameter               |
| 6A  | 83  | HMAC Key with specified ID is not found |

### 2. OTP functions

##### 2.1. Calculate HMAC (CALCULATE_HMAC)

##### Command

| Field | Value | Description          |
|-------|-------|----------------------|
| CLA   | 00    |                      |
| INS   | 01    | Calculate HMAC       |
| P1    | VAR   | Algorithm type       |
| P2    | VAR   | HMAC Key ID (0..255) |
| Lc    | 8     | Counter data length  |
| Data  |       | Counter data value   |
| Le    | VAR   | HMAC value           |

##### Description

Calculate HMAC value using specified HMAC Key and counter data

##### P1 values

| Value | Description      |
|-------|------------------|
| 18    | ALG_HMAC_SHA1    |
| 19    | ALG_HMAC_SHA_256 |
| 1A    | ALG_HMAC_SHA_384 |
| 1B    | ALG_HMAC_SHA_512 |

##### Response

| Field | Value | Description           |
|-------|-------|-----------------------|
| Data  |       | Calculated HMAC value |

##### Response codes

| SW1 | SW2 | Description                             |
|-----|-----|-----------------------------------------|
| 90  | 00  | OK                                      |
| 6D  | 00  | Instruction no supported or invalid     |
| 6A  | 86  | Incorrect P1/P2 parameter               |
| 6A  | 83  | HMAC Key with specified ID is not found |
| 67  | 00  | Wrong length                            |

#### 2.2. Calculate HMAC-Base One-Time Password (CALCULATE_OTP)

##### Command

| Field | Value | Description          |
|-------|-------|----------------------|
| CLA   | 00    |                      |
| INS   | 04    | Calculate HOTP value |
| P1    | VAR   | Algorithm type       |
| P2    | VAR   | HMAC Key ID (0..255) |
| Lc    | 8     | Counter data length  |
| Data  |       | Counter data value   |
| Le    | VAR   | HOTP value           |

##### Description

Calculate HOTP value using specified HMAC Key and counter data according
to [RFC 4226](https://datatracker.ietf.org/doc/html/rfc4226)

##### P1 values

| Value | Description      |
|-------|------------------|
| 18    | ALG_HMAC_SHA1    |
| 19    | ALG_HMAC_SHA_256 |
| 1A    | ALG_HMAC_SHA_384 |
| 1B    | ALG_HMAC_SHA_512 |

##### Response

| Field | Value | Description           |
|-------|-------|-----------------------|
| Data  |       | Calculated HOTP value |

##### Response codes

| SW1 | SW2 | Description                             |
|-----|-----|-----------------------------------------|
| 90  | 00  | OK                                      |
| 6D  | 00  | Instruction no supported or invalid     |
| 6A  | 86  | Incorrect P1/P2 parameter               |
| 6A  | 83  | HMAC Key with specified ID is not found |
| 67  | 00  | Wrong length                            |