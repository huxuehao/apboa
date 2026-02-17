<template>
  <div class="body">
    <div class="mars"></div>
    <img src="../assets/images/notfound/404.svg" class="logo-404" alt="404">
    <img src="../assets/images/notfound/meteor.svg" class="meteor" alt="Meteor">
    <p class="title">Oh no!!</p>
    <p class="subtitle">
      <!-- 哎呀！<br>您访问的页面不存在。 -->
    </p>
    <div style="text-align: center;">
      <a class="btn-back" href="/#/agent">返回</a>
    </div>
    <img src="../assets/images/notfound/astronaut.svg" class="astronaut" alt="Astronaut">
    <img src="../assets/images/notfound/spaceship.svg" class="spaceship" alt="Spaceship">
  </div>
</template>

<style lang="scss" scoped>
@keyframes floating {
  from {
    transform: translateY(0px);
  }

  65% {
    transform: translateY(15px);
  }

  to {
    transform: translateY(0px);
  }
}

.body {
  background-image: url("../assets/images/notfound/star.svg") , linear-gradient(to bottom, #1a1d23, #494d55);
  background-color: var(--global-bg-color);
  height: 100vh;
  margin: 0;
  background-attachment: fixed;
  overflow: hidden;
}

.mars {
  left: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  height: 27vmin;
  background: url("../assets/images/notfound/mars.svg") no-repeat bottom center;
  background-size: cover;
}

.logo-404 {
  position: absolute;
  margin-left: auto;
  margin-right: auto;
  left: 0;
  right: 0;
  top: 16vmin;
  width: 30vmin;
}

@media (max-width: 480px) and (min-width: 320px) {
  .logo-404 {
    top: 45vmin;
  }
}

.meteor {
  position: absolute;
  right: 2vmin;
  top: 16vmin;
}

.title {
  color: #CFD8DC;
  font-family: "Nunito", sans-serif;
  font-weight: 600;
  text-align: center;
  font-size: 4vmin;
  margin-top: 31vmin;
}

@media (max-width: 480px) and (min-width: 320px) {
  .title {
    margin-top: 65vmin;
  }
}

.subtitle {
  color: #CFD8DC;
  font-family: "Nunito", sans-serif;
  font-weight: 400;
  text-align: center;
  font-size: 2.5vmin;
  margin-top: 10vmin;
  margin-bottom: 9vmin;
}

.btn-back {
  border: 1px solid rgb(131, 131, 131);
  color: #CFD8DC;
  font-size: 2vmin;
  padding: 1vmin 1.5vmin;
  font-family: "Nunito", sans-serif;
  text-decoration: none;
  border-radius: 1vmin;
}

.btn-back:hover {
  background: #272d30;
}

@media (max-width: 480px) and (min-width: 320px) {
  .btn-back {
    font-size: 3.5vmin;
  }
}

.astronaut {
  position: absolute;
  top: 18vmin;
  left: 10vmin;
  height: 30vmin;
  animation: floating 3s infinite ease-in-out;
}

@media (max-width: 480px) and (min-width: 320px) {
  .astronaut {
    top: 2vmin;
  }
}

.spaceship {
  position: absolute;
  bottom: 15vmin;
  right: 24vmin;
}

@media (max-width: 480px) and (min-width: 320px) {
  .spaceship {
    width: 45vmin;
    bottom: 18vmin;
  }
}</style>
