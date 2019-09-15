#include "volctl.h"

#include <alsa/asoundlib.h>
#include <cmath>

const char *card = "default";
const char *selem_name = "Master";

snd_mixer_elem_t *getElement(snd_mixer_t *handle) {
    snd_mixer_attach(handle, card);
    snd_mixer_selem_register(handle, nullptr, nullptr);
    snd_mixer_load(handle);

    snd_mixer_selem_id_t *sid;
    snd_mixer_selem_id_alloca(&sid);
    snd_mixer_selem_id_set_index(sid, 0);
    snd_mixer_selem_id_set_name(sid, selem_name);
    return snd_mixer_find_selem(handle, sid);
}

JNIEXPORT jint JNICALL Java_net_bjoernpetersen_volctl_VolumeControl_getVolumeNative
        (JNIEnv *, jobject) {
    snd_mixer_t *handle;
    snd_mixer_open(&handle, 0);
    auto elem = getElement(handle);

    long min, max;
    snd_mixer_selem_get_playback_volume_range(elem, &min, &max);
    long result;
    snd_mixer_selem_get_playback_volume(elem, SND_MIXER_SCHN_MONO, &result);
    result = lround((double) result * 100.0 / (double) max);

    snd_mixer_close(handle);

    return result;
}

JNIEXPORT void JNICALL Java_net_bjoernpetersen_volctl_VolumeControl_setVolumeNative
        (JNIEnv *, jobject, jint value) {
    snd_mixer_t *handle;
    snd_mixer_open(&handle, 0);
    auto elem = getElement(handle);

    long min, max;
    snd_mixer_selem_get_playback_volume_range(elem, &min, &max);
    snd_mixer_selem_set_playback_volume_all(elem, value * max / 100);

    snd_mixer_close(handle);
}
