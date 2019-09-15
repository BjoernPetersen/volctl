#include "volctl.h"

#include "combaseapi.h"
#include "mmdeviceapi.h"
#include "endpointvolume.h"

const IID CLSID_MMDeviceEnumerator = __uuidof(MMDeviceEnumerator);
const IID IID_IMMDeviceEnumerator = __uuidof(IMMDeviceEnumerator);
const IID IID_IAudioEndpointVolume = __uuidof(IAudioEndpointVolume);

void initialize() {
    CoInitializeEx(nullptr, COINIT_MULTITHREADED);
}

IMMDeviceEnumerator *getEnumerator() {
    initialize();
    IMMDeviceEnumerator *enumerator;
    CoCreateInstance(
            CLSID_MMDeviceEnumerator, nullptr,
            CLSCTX_ALL, IID_IMMDeviceEnumerator,
            (void **) &enumerator);
    return enumerator;
}

IMMDevice *getDefaultDevice() {
    auto enumerator = getEnumerator();
    IMMDevice *device;
    enumerator->GetDefaultAudioEndpoint(
            eRender,
            eMultimedia,
            &device);
    return device;
}

IAudioEndpointVolume *getEndpointVolume() {
    auto device = getDefaultDevice();
    IAudioEndpointVolume *volume;
    device->Activate(
            IID_IAudioEndpointVolume,
            CLSCTX_ALL,
            nullptr,
            (void **) &volume
    );
    return volume;
}

JNIEXPORT jint JNICALL Java_net_bjoernpetersen_volctl_VolumeControl_getVolumeNative
        (JNIEnv *, jobject) {
    auto volume = getEndpointVolume();

    float result;
    volume->GetMasterVolumeLevelScalar(&result);

    return (int) (result * 100);
}

JNIEXPORT void JNICALL Java_net_bjoernpetersen_volctl_VolumeControl_setVolumeNative
        (JNIEnv *, jobject, jint value) {
    auto volume = getEndpointVolume();
    float floatValue = value / 100.0f;
    volume->SetMasterVolumeLevelScalar(floatValue, nullptr);
}
