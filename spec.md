# Crow Transfer Protocol Specification [TOP]

1. [Overview][over]
2. [Requests][req]
    1. [Record Requests][record-req]
    2. [Blob Requests][blob-req]
    3. [Chunk Requests][chunk-req]
    4. [Digest Requests][digest-req]
3. [Responses][res]
    1. [Record Responses][record-res]
    2. [Blob Responses][blob-res]
    3. [Chunk Responses][chunk-res]
    4. [Digest Responses][digest-res]

## 1 Overview [over]

The *Crow* protocol is used to transfer binary resources, either as a single blob or in chunks. A single persistent connection can be used to make multiple requests, but the server is of course free to terminate sessions as it sees fit.

During a session, resources are identified by integral ID. While an ID may very well persist accross sessions, this is not guaranteed.

The 32-bit ID is part of a resource's 24-byte record, the other entries being a CRC32 checksum, its 64-bit size and a 64-bit timestamp encoded as milliseconds since the UNIX epoch.

Records can be requested by name, but the resources associated with a given name may change over time. Therefore, it is imperative to also compare timestamps when splitting requests across sessions.

To ensure integrity of transferred resource, a message digest may be requested. While clients are free to request arbitrary algorithms, only SHA-256 is required.

## 2 Requests [req]

Requests happen in terms of request frames, a sequence of requests of uniform type that is supposed to fit into a single TCP segment.

```
stream = <magic.4> <frame.?>+
magic  = "crow"
frame  = <type.1> <count.1> <head.?> <body.?>
type   = {zyyxxxxx}
```

| bits | hex | ascii | description                   |
|:----:|:---:|:-----:|:------------------------------|
| `z`  |  1  |   -   | keep alive                    |
| `y`  |  1  |   -   | use alternative body encoding |
| `x`  | 11  |  DC1  | request record                |
| `x`  | 12  |  DC2  | request blob                  |
| `x`  | 13  |  DC3  | request chunk                 |
| `x`  | 14  |  DC4  | request digest                |

Note
:   Frame headers encoded according to the following scheme will be at most 1024 bytes long.

### 2.1 Record Requests [record-req]

Requests the record for a given resource by its UTF8-encoded name, optionally compressed using the `deflate` algorithm.

```
head = <len.2>+ <size.4>
body = <raw.?>
     | <deflated.?>
```

### 2.2 Blob Requests [blob-req]

Request transmission of entire resources. Does not have a body.

```
head = <id.4>+
body = X
```

### 2.3 Chunk Requests [chunk-req]

Requests a chunk of a given resource. Optionally, a variable-length encoding may be used to represent offsets and lengths.

```
head = <id.4>+ <size.2>
body = [ <off.8> <len.8> ]+
     | [ <off.var> <len.var> ]+
```

#### 2.3.1 Alternative Variable-Length Integer Encoding [varint-enc]

```
 32ki  0xxxxxxx xxxxxxxx
  1Gi  10xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
2**62  11xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
```

### 2.4 Digest Requests [digest-req]

Requests a message digest for a given resource. The body contains a comma-separated list of algorithms, from which the server will choose as it sees fit. The default algorithm SHA-256 is always present and does not need to be listed explicitly.

```
head = <id.4>+ <size.2>
body = <raw.?>
     | <deflated.?>
```

## 3 Responses [res]

```
stream   = <response.?>+
response = [ <type.1> <data.?> <crc.4>? ]+
type     = {zyyxxxxx}
```

The response type pertains to individual requests.

| bits | hex | ascii | description                   |
|:----:|:---:|:-----:|:------------------------------|
| `z`  |  1  |   -   | CRC32 checksum after data     |
| `y`  |  1  |   -   | use alternative data encoding |
| `x`  | 04  |  EOT  | abort transmission            |
| `x`  | 06  |  ACK  | request accepted              |
| `x`  | 15  |  NAK  | request rejected              |

### 3.1 Record Responses [record-res]

Transfer of a CRC32 checksum is not recommended.

```
data = <id.4> <crc.4> <size.8> <timestamp.8>
```

### 3.2 Blob Responses [blob-res]

Transfer of a CRC32 checksum is not recommended as it has already been transferred as part of the record.

```
data = <raw.?>
     | <deflated.?>
```

### 3.3 Chunk Responses [chunk-res]

Transfer of a CRC32 checksum is recommended.

```
data = <raw.?>
     | <deflated.?>
```

### 3.4 Digest Responses [digest-res]

Transfer of a CRC32 checksum is optional. The used algorithm is identified by its index in the request list, where a value of 0 corresponds to the default algorithm SHA-256.

```
data = <algorithm.1> <digest.?>
```
