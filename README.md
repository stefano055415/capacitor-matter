# @falconeta/capacitor-plugin-matter

# WORK IN PROGRESS!

plugin that handle the Matter Application protocol

## Install

```bash
npm install @falconeta/capacitor-plugin-matter
npx cap sync
```

## API

<docgen-index>

* [`configure(...)`](#configure)
* [`clear()`](#clear)
* [`startCommissioning(...)`](#startcommissioning)
* [`commandOnOff(...)`](#commandonoff)
* [`getEndpoint(...)`](#getendpoint)
* [`getCluster(...)`](#getcluster)
* [`getAttribute(...)`](#getattribute)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### configure(...)

```typescript
configure(options: { deviceControllerKey: string; caRootCert: string; fabricId: string; vendorId: number; }) => Promise<void>
```

| Param         | Type                                                                                                  |
| ------------- | ----------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ deviceControllerKey: string; caRootCert: string; fabricId: string; vendorId: number; }</code> |

--------------------


### clear()

```typescript
clear() => Promise<void>
```

--------------------


### startCommissioning(...)

```typescript
startCommissioning(options: { deviceId: string; }) => Promise<{ deviceType: string; }>
```

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ deviceId: string; }</code> |

**Returns:** <code>Promise&lt;{ deviceType: string; }&gt;</code>

--------------------


### commandOnOff(...)

```typescript
commandOnOff(options: { deviceId: string; value: boolean; endpointId: number; }) => Promise<void>
```

| Param         | Type                                                                   |
| ------------- | ---------------------------------------------------------------------- |
| **`options`** | <code>{ deviceId: string; value: boolean; endpointId: number; }</code> |

--------------------


### getEndpoint(...)

```typescript
getEndpoint<T>(options: { deviceId: string; endpointId: number; }) => Promise<{ data: T; }>
```

| Param         | Type                                                   |
| ------------- | ------------------------------------------------------ |
| **`options`** | <code>{ deviceId: string; endpointId: number; }</code> |

**Returns:** <code>Promise&lt;{ data: T; }&gt;</code>

--------------------


### getCluster(...)

```typescript
getCluster<T>(options: { deviceId: string; endpointId: number; clusterId: number; }) => Promise<{ data: T; }>
```

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code>{ deviceId: string; endpointId: number; clusterId: number; }</code> |

**Returns:** <code>Promise&lt;{ data: T; }&gt;</code>

--------------------


### getAttribute(...)

```typescript
getAttribute<T>(options: { deviceId: string; endpointId: number; clusterId: number; attributeId: number; }) => Promise<{ data: T; }>
```

| Param         | Type                                                                                           |
| ------------- | ---------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ deviceId: string; endpointId: number; clusterId: number; attributeId: number; }</code> |

**Returns:** <code>Promise&lt;{ data: T; }&gt;</code>

--------------------

</docgen-api>
