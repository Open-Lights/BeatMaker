# BeatMaker
Creates and edits beat files for [Open Lights Core](https://github.com/QPCrummer/OpenLightsCore).\
This is a continuation of my [BeatFileEditor](https://github.com/QPCrummer/BeatFileEditor).\
\
A guide for using this software can be found [here](https://github.com/QPCrummer/OpenLightsCore/wiki/Open-Lights-BeatMaker-Tour)
---

## About
Open Lights BeatMaker allows editing beats based on the inputted song.
Some features include:
- Beat recording: Record beats with your mouse while the song is playing
- Onset and BPM-based beat generation
- Custom lighting effects **(WIP)**
- In-depth beat editing: Chart editor for millisecond accuracy
- Per-channel editing: Edit for one channel, or edit for many channels at once
- Light show preview: See how your light show will appear as you play the song
- Simple User Interface: Edit a beat interval just by typing
- Loading charts: Ability to load charts from beat files
- Demucs AI integration to allow for instrument-specific beat generation

---

## Performance
This software was oriented to be used on low-end hardware. The maximum ram consumption should be around 300MB or less.
This software can be CPU-heavy at times, but these instances are infrequent.
If installed with Demucs, this software can be very demanding. Read the requirements below to see if your PC qualifies.

---

## Requirements
### Just Open Lights Beat Maker
- Storage: 200MB
- Memory: 500MB
- Operating System: Any
### Open Lights Beat Maker with Demucs AI
- Storage: 1.2GB
- Memory: 8GB
- GPU: Preferably one that supports CUDA (Nvidia GTX / RTX), but it isn't required
- Operating System: Windows 10/11

---

## File Output
This json file will appear named after your song in `openlights/saves/song_name.json`
```json
{
  "0, 1": {      # Channels 0 and 1
    "1040": 1,   # Light On at 1040 milliseconds
    "2000": 0,   # Light Off at 2000 milliseconds
    "2970": 1,
    "3930": 0
  }
}
```
