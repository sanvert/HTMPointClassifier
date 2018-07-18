#include <GL/glut.h>
#include <stdio.h>
#include <math.h>
#include <string>
#include <stdexcept>

int depth = 0;

static GLfloat vdata[6][3] = {
   {1.f, 0.f, 0.f}, {-1.f, 0.f, 0.f}, {0.f, 1.f, 0.f},
   {0.f,-1.f, 0.f}, {0.f, 0.f, 1.f}, {0.f, 0.f,-1.f}
};
static GLuint tindices[8][3] = {
   {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2},
   {1, 2, 5}, {1, 5, 3}, {1, 3, 4}
};

void error (std::string s)
{
  throw std::runtime_error(s);
}

void normalize(float v[3])
{
   GLfloat d = sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
   if (d == 0.0) {
      error("zero len vector");
      return;
   }
   v[0] /= d; v[1] /= d; v[2] /= d;
}

double r2()
{
    return (double)rand() / (double)RAND_MAX ;
}

void drawtriangle(float *v1, float *v2, float *v3)
{
      glColor3f(1.0 - (r2() / 1.9), 0.0, 0.0);
      glNormal3fv(v1);
      glVertex3fv(v1);
      glNormal3fv(v2);
      glVertex3fv(v2);
      glNormal3fv(v3);
      glVertex3fv(v3);
}

void subdivide(float *v1, float *v2, float *v3, long depth)
{
   GLfloat v12[3], v23[3], v31[3];
   GLint i;

   if (depth == 0) {
      drawtriangle(v1, v2, v3);
      return;
   }
   for (i = 0; i < 3; i++) {
      v12[i] = v1[i]+v2[i];
      v23[i] = v2[i]+v3[i];
      v31[i] = v3[i]+v1[i];
   }
   normalize(v12);
   normalize(v23);
   normalize(v31);
   subdivide(v1, v12, v31, depth-1);
   subdivide(v2, v23, v12, depth-1);
   subdivide(v3, v31, v23, depth-1);
   subdivide(v12, v23, v31, depth-1);
}

void display_polyhedron(void) {
    int i = 0;
    glClear(GL_COLOR_BUFFER_BIT);
    glBegin(GL_TRIANGLES);
    for (i = 0; i < 8; i++) {
        subdivide(&vdata[tindices[i][0]][0],
                    &vdata[tindices[i][1]][0],
                    &vdata[tindices[i][2]][0], depth);
    }
    glEnd();
    glutSwapBuffers();
}

int main (int argc, char **argv)
{
    depth = atoi(argv[1]);
     //Initialize GLUT
    glutInit(&argc, argv);
    //double buffering used to avoid flickering problem in animation
    glutInitDisplayMode (GLUT_DOUBLE | GLUT_RGB);
    // window size
    glutInitWindowSize(500,500);
    // create the window
    char *title;
    asprintf(&title,"%s%d", "HTM Index Level ", (depth + 1));
    glutCreateWindow(title);

    glClearColor(0.0, 0.0, 0.0, 0.0);         // black background
    glMatrixMode(GL_MODELVIEW);              // setup viewing projection
    glLoadIdentity();                           // start with identity matrix

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glutDisplayFunc(display_polyhedron);

    glutMainLoop();
    return 0;
}