/*******************************************************************************
 * Copyright (c) 2008 Radoslav Gerganov and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Radoslav Gerganov - initial API and implementation
 *    Radoslav Gerganov - [238773] [WinCE] Implement IRAPISession#CeRapiInvoke 
 *******************************************************************************/
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_eclipse_tm_internal_rapi_RapiSession */

#ifndef _Included_org_eclipse_tm_internal_rapi_RapiSession
#define _Included_org_eclipse_tm_internal_rapi_RapiSession
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRapiInit
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRapiInit
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRapiUninit
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRapiUninit
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRapiGetError
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRapiGetError
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeGetLastError
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeGetLastError
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeCreateFile
 * Signature: (ILjava/lang/String;IIII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeCreateFile
  (JNIEnv *, jobject, jint, jstring, jint, jint, jint, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeReadFile
 * Signature: (II[BI[I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeReadFile
  (JNIEnv *, jobject, jint, jint, jbyteArray, jint, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeWriteFile
 * Signature: (II[BI[I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeWriteFile
  (JNIEnv *, jobject, jint, jint, jbyteArray, jint, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeCloseHandle
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeCloseHandle
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeCopyFile
 * Signature: (ILjava/lang/String;Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeCopyFile
  (JNIEnv *, jobject, jint, jstring, jstring, jboolean);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeDeleteFile
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeDeleteFile
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeMoveFile
 * Signature: (ILjava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeMoveFile
  (JNIEnv *, jobject, jint, jstring, jstring);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeCreateDirectory
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeCreateDirectory
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRemoveDirectory
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRemoveDirectory
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeFindFirstFile
 * Signature: (ILjava/lang/String;Lorg/eclipse/tm/rapi/RapiFindData;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeFindFirstFile
  (JNIEnv *, jobject, jint, jstring, jobject);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeFindNextFile
 * Signature: (IILorg/eclipse/tm/rapi/RapiFindData;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeFindNextFile
  (JNIEnv *, jobject, jint, jint, jobject);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeFindClose
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeFindClose
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeFindAllFiles
 * Signature: (ILjava/lang/String;I[I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeFindAllFiles
  (JNIEnv *, jobject, jint, jstring, jint, jintArray, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeFindAllFilesEx
 * Signature: (III[Lorg/eclipse/tm/rapi/RapiFindData;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeFindAllFilesEx
  (JNIEnv *, jobject, jint, jint, jint, jobjectArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeGetFileAttributes
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeGetFileAttributes
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeGetFileSize
 * Signature: (II[I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeGetFileSize
  (JNIEnv *, jobject, jint, jint, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeGetFileTime
 * Signature: (II[J[J[J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeGetFileTime
  (JNIEnv *, jobject, jint, jint, jlongArray, jlongArray, jlongArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeSetFileAttributes
 * Signature: (ILjava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeSetFileAttributes
  (JNIEnv *, jobject, jint, jstring, jint);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeSetFileTime
 * Signature: (II[J[J[J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeSetFileTime
  (JNIEnv *, jobject, jint, jint, jlongArray, jlongArray, jlongArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeCreateProcess
 * Signature: (ILjava/lang/String;Ljava/lang/String;ILorg/eclipse/tm/rapi/ProcessInformation;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeCreateProcess
  (JNIEnv *, jobject, jint, jstring, jstring, jint, jobject);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRapiInvoke
 * Signature: (ILjava/lang/String;Ljava/lang/String;[B[I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRapiInvoke
  (JNIEnv *, jobject, jint, jstring, jstring, jbyteArray, jintArray);

/*
 * Class:     org_eclipse_tm_internal_rapi_RapiSession
 * Method:    CeRapiInvokeEx
 * Signature: (I[B)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_tm_internal_rapi_RapiSession_CeRapiInvokeEx
  (JNIEnv *, jobject, jint, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
