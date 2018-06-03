![image](https://github.com/skateboard1991/OpenGlCamera/blob/master/show.gif)

# OpenGlCamera
使用opengles用作相机预览，添加视频水印，录制并保存为MP4文件，目前尚不支持音轨。

用法比较简单
    
    RecordActivity.startRecordActivity(context: Context, bitmap: Bitmap, totalTime: Float, outputFile: File,waterX:Float=-1f,waterY:Float=1F)
    
    
waterX和waterY是水印左下角坐标的位置，范围为[-1,1]    
