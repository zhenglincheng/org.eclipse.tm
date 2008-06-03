/*******************************************************************************
 * Copyright (c) 2008 Radoslav Gerganov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Radoslav Gerganov - initial API and implementation
 *******************************************************************************/
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_eclipse_tm_internal_rapi_RapiDevice */

#ifndef _Included_org_eclipse_tm_internal_rapi_RapiDevice
#define _Included_org_eclipse_tm_internal_rapi_RapiDevice
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_tm_internal_rapi_RapiDevice
 * Method:    CreateSession
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiDevice_CreateSession
  (JNIEnv *, jobject, jint, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiDevice
 * Method:    GetConnectionInfo
 * Signature: (ILorg/eclipse/tm/rapi/RapiConnectionInfo;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiDevice_GetConnectionInfo
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiDevice
 * Method:    GetDeviceInfo
 * Signature: (ILorg/eclipse/tm/rapi/RapiDeviceInfo;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiDevice_GetDeviceInfo
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiDevice
 * Method:    GetConnectStat
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiDevice_GetConnectStat
  (JNIEnv *, jobject, jint, jintArray);

#ifdef __cplusplus
}
#endif
#endif