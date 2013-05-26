varying float varyingHeight;

void main(){
    varyingHeight = gl_Vertex.y;
    gl_Position = ftransform();
}