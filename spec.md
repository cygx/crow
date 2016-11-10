# Crow Transfer Protocol Specification

## Requests

```
stream = <magic.4> <frame.?>+
magic  = "crow"
frame  = <type.1> <count.1> <head.?> <body.?>
type   = {zyyxxxxx}
```

Request types pertain to the whole frame.

| bits | hex | ascii | description                   |
|:----:|:---:|:-----:|:------------------------------|
| `z`  |  1  |   -   | keep alive                    |
| `y`  |  1  |   -   | use alternative body encoding |
| `x`  | 11  |  DC1  | request record                |
| `x`  | 12  |  DC2  | request blob                  |
| `x`  | 13  |  DC3  | request chunk                 |
| `x`  | 14  |  DC4  | request digest                |

### Record Requests

```
head = [ <len.2>+ <size.4> ]+ <size.4>
body = <raw.?>
     | <deflated.?>
```

### Blob Requests

```
head = <id.4>+
body = X
```

### Chunk Requests

```
head = <id.4>+ <size.2>
body = [ <off.8> <len.8> ]+
     | [ <off.var> <len.var> ]+
```

#### Alternative Variable-Length Integer Encoding

```
 32ki  0xxxxxxx xxxxxxxx
  1Gi  10xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
2**62  11xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
```

### Digest Requests

```
head = [ <id.4> <len.2> ]+ <size.2>
body = <raw>
     | <deflated>
```

## Responses

```
stream  = <response.?>+
respone = [ <type.1> <data.?> <crc.4>? ]+
type    = {zyyxxxxx}
```

Reponse types pertain to individual requests.

| bits | hex | ascii | description                   |
|:----:|:---:|:-----:|:------------------------------|
| `z`  |  1  |   -   | CRC32 checksum after data     |
| `y`  |  1  |   -   | use alternative body encoding |
| `x`  | 04  |  EOT  | abort transmission            |
| `x`  | 15  |  NAK  | request rejected              |
| `x`  | 06  |  ACK  | request accepted              |
