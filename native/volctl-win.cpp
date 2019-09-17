#include "volctl.h"

#include "cmath"
#include "atlbase.h"
#include "combaseapi.h"
#include "endpointvolume.h"
#include "mmdeviceapi.h"

const IID CLSID_MMDeviceEnumerator = __uuidof(MMDeviceEnumerator);
const IID IID_IAudioEndpointVolume = __uuidof(IAudioEndpointVolume);

CComPtr<IAudioEndpointVolume> getEndpointVolume() {
    CoInitializeEx(nullptr, COINIT_MULTITHREADED);
    CComPtr<IMMDeviceEnumerator> enumerator;
    enumerator.CoCreateInstance(
            CLSID_MMDeviceEnumerator, nullptr,
            CLSCTX_ALL);

    CComPtr<IMMDevice> device;
    enumerator->GetDefaultAudioEndpoint(
            eRender,
            eMultimedia,
            &device);

    CComPtr<IAudioEndpointVolume> volume;
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
    CoUninitialize();

    return lround(result * 100.0);
}

JNIEXPORT void JNICALL Java_net_bjoernpetersen_volctl_VolumeControl_setVolumeNative
        (JNIEnv *, jobject, jint value) {
    auto volume = getEndpointVolume();
    float floatValue = value / 100.0f;
    volume->SetMasterVolumeLevelScalar(floatValue, nullptr);
    CoUninitialize();
}
